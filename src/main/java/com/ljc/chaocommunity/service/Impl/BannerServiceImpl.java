package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.BannerMapper;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.pojo.dto.CreateBannerDTO;
import com.ljc.chaocommunity.pojo.dto.UpdateBannerDTO;
import com.ljc.chaocommunity.pojo.entity.Banner;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.vo.AdminBannerVO;
import com.ljc.chaocommunity.pojo.vo.BannerVO;
import com.ljc.chaocommunity.service.BannerService;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BannerServiceImpl implements BannerService {

    @Autowired
    private BannerMapper bannerMapper;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 主页轮播图缓存 key（管理端"清空缓存"也引用此常量） */
    public static final String HOME_BANNERS_CACHE_KEY = "home:banners";
    /** 主页缓存 TTL：30 分钟 */
    private static final long HOME_CACHE_TTL = 30;
    /** 空缓存 TTL：30 秒（防穿透） */
    private static final long HOME_EMPTY_CACHE_TTL = 30;

    // ==================== 用户端 ====================

    /**
     * 查询展示中的轮播图（Redis 缓存，Cache-Aside；不随业务失效，管理端手动清空）
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<BannerVO> getUserBanners() {
        // 1. 先查 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(HOME_BANNERS_CACHE_KEY);
        if (cached instanceof List<?>) {
            return (List<BannerVO>) cached;
        }

        // 2. 未命中：查库
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSort);
        List<Banner> banners = bannerMapper.selectList(wrapper);
        List<BannerVO> voList = banners.stream().map(b -> {
            BannerVO vo = new BannerVO();
            BeanUtils.copyProperties(b, vo);
            return vo;
        }).collect(Collectors.toList());

        // 3. 写缓存：空结果也缓存短 TTL 防穿透
        if (voList.isEmpty()) {
            redisTemplate.opsForValue().set(HOME_BANNERS_CACHE_KEY, voList, HOME_EMPTY_CACHE_TTL, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(HOME_BANNERS_CACHE_KEY, voList, HOME_CACHE_TTL, TimeUnit.MINUTES);
        }
        return voList;
    }

    // ==================== 管理端 ====================

    @Override
    public List<AdminBannerVO> getAdminBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Banner::getSort);
        List<Banner> banners = bannerMapper.selectList(wrapper);
        return banners.stream().map(b -> {
            AdminBannerVO vo = new AdminBannerVO();
            BeanUtils.copyProperties(b, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void create(CreateBannerDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 1. 校验文件
        FileRecord fileRecord = fileRecordMapper.selectById(dto.getFileId());
        if (fileRecord == null) {
            throw new BusinessException("文件不存在");
        }
        if (!fileRecord.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权使用该文件");
        }
        if (fileRecord.getStatus() == 1) {
            throw new BusinessException("文件已经被使用");
        }
        if (!fileRecord.getFilePath().startsWith("temp/")) {
            throw new BusinessException("非法文件");
        }

        // 2. 移动文件 temp/ → banner/
        String oldObjectKey = fileRecord.getFilePath();
        String newObjectKey = oldObjectKey.replace("temp/", "banner/");
        OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

        // 3. 更新 file_record
        fileRecord.setFilePath(moveResult.objectKey());
        fileRecord.setUrl(moveResult.url());
        fileRecord.setStatus(1);
        fileRecordMapper.updateById(fileRecord);

        // 4. 插入 banner
        Banner banner = new Banner();
        banner.setTitle(dto.getTitle());
        banner.setImgUrl(moveResult.url());
        banner.setFileId(fileRecord.getId());
        banner.setLinkUrl(dto.getLinkUrl());
        banner.setSort(dto.getSort() != null ? dto.getSort() : 0);
        banner.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        bannerMapper.insert(banner);
    }

    @Override
    @Transactional
    public void update(UpdateBannerDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 1. 查询旧 banner
        Banner banner = bannerMapper.selectById(dto.getId());
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }

        // 2. 处理图片：fileId 变了才处理
        if (dto.getFileId() != null && !dto.getFileId().equals(banner.getFileId())) {
            FileRecord newFileRecord = fileRecordMapper.selectById(dto.getFileId());
            if (newFileRecord == null) {
                throw new BusinessException("文件不存在");
            }
            if (!newFileRecord.getUserId().equals(currentUserId)) {
                throw new BusinessException("无权使用该文件");
            }
            if (newFileRecord.getStatus() == 1) {
                throw new BusinessException("文件已经被使用");
            }
            if (!newFileRecord.getFilePath().startsWith("temp/")) {
                throw new BusinessException("非法文件");
            }

            // 移动新文件
            String oldObjectKey = newFileRecord.getFilePath();
            String newObjectKey = oldObjectKey.replace("temp/", "banner/");
            OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

            // 释放旧文件
            FileRecord oldFileRecord = fileRecordMapper.selectById(banner.getFileId());
            if (oldFileRecord != null) {
                oldFileRecord.setStatus(0);
                fileRecordMapper.updateById(oldFileRecord);
            }

            // 更新新文件
            newFileRecord.setFilePath(moveResult.objectKey());
            newFileRecord.setUrl(moveResult.url());
            newFileRecord.setStatus(1);
            fileRecordMapper.updateById(newFileRecord);

            banner.setFileId(newFileRecord.getId());
            banner.setImgUrl(moveResult.url());
        }

        // 3. 更新其他字段
        if (dto.getTitle() != null) {
            banner.setTitle(dto.getTitle());
        }
        if (dto.getLinkUrl() != null) {
            banner.setLinkUrl(dto.getLinkUrl());
        }
        if (dto.getSort() != null) {
            banner.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            banner.setStatus(dto.getStatus());
        }

        bannerMapper.updateById(banner);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException("轮播图不存在");
        }

        // 释放文件
        if (banner.getFileId() != null) {
            FileRecord fileRecord = fileRecordMapper.selectById(banner.getFileId());
            if (fileRecord != null && fileRecord.getStatus() == 1) {
                fileRecord.setStatus(0);
                fileRecordMapper.updateById(fileRecord);
            }
        }

        // 逻辑删除
        bannerMapper.deleteById(id);
    }
}
