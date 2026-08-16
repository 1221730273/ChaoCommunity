package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.AnnouncementMapper;
import com.ljc.chaocommunity.pojo.dto.AnnouncementDTO;
import com.ljc.chaocommunity.pojo.entity.Announcement;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.AdminAnnouncementVO;
import com.ljc.chaocommunity.pojo.vo.AnnouncementVO;
import com.ljc.chaocommunity.service.AnnouncementService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** 主页最新公告缓存 key 前缀（按 limit 区分，管理端"清空缓存"用前缀匹配删除） */
    public static final String HOME_ANNOUNCEMENTS_CACHE_KEY_PREFIX = "home:announcements:";
    /** 主页缓存 TTL：30 分钟 */
    private static final long HOME_CACHE_TTL = 30;
    /** 空缓存 TTL：30 秒（防穿透） */
    private static final long HOME_EMPTY_CACHE_TTL = 30;

    // ==================== 用户端 ====================

    @Override
    public PageResult<AnnouncementVO> list(int page, int size) {
        Page<Announcement> p = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 0)
                .orderByDesc(Announcement::getIsTop)
                .orderByAsc(Announcement::getSort)
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> result = announcementMapper.selectPage(p, wrapper);
        List<AnnouncementVO> voList = new ArrayList<>();
        for (Announcement a : result.getRecords()) {
            AnnouncementVO vo = new AnnouncementVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setType(a.getType());
            vo.setIsTop(a.getIsTop());
            vo.setViewCount(a.getViewCount());
            vo.setCreateTime(a.getCreateTime());
            voList.add(vo);
        }
        return new PageResult<>(result.getTotal(), voList);
    }

    @Override
    @Transactional
    public AnnouncementVO getDetail(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null || a.getStatus() != 0) {
            throw new BusinessException("公告不存在");
        }

        // 浏览量 +1
        Announcement update = new Announcement();
        update.setId(id);
        update.setViewCount(a.getViewCount() + 1);
        announcementMapper.updateById(update);

        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setType(a.getType());
        vo.setIsTop(a.getIsTop());
        vo.setViewCount(a.getViewCount() + 1);
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }

    /**
     * 查询最新公告（Redis 缓存，Cache-Aside；不随业务失效，管理端手动清空）
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<AnnouncementVO> getLatest(int limit) {
        String key = HOME_ANNOUNCEMENTS_CACHE_KEY_PREFIX + limit;

        // 1. 先查 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof List<?>) {
            return (List<AnnouncementVO>) cached;
        }

        // 2. 未命中：查库
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 0)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT " + limit);
        List<Announcement> list = announcementMapper.selectList(wrapper);
        List<AnnouncementVO> voList = list.stream().map(a -> {
            AnnouncementVO vo = new AnnouncementVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setType(a.getType());
            vo.setIsTop(a.getIsTop());
            vo.setViewCount(a.getViewCount());
            vo.setCreateTime(a.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        // 3. 写缓存：空结果也缓存短 TTL 防穿透
        if (voList.isEmpty()) {
            redisTemplate.opsForValue().set(key, voList, HOME_EMPTY_CACHE_TTL, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, voList, HOME_CACHE_TTL, TimeUnit.MINUTES);
        }
        return voList;
    }

    // ==================== 管理端 ====================

    @Override
    public List<AdminAnnouncementVO> adminList() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getIsTop)
                .orderByAsc(Announcement::getSort)
                .orderByDesc(Announcement::getCreateTime);
        List<Announcement> list = announcementMapper.selectList(wrapper);
        List<AdminAnnouncementVO> voList = new ArrayList<>();
        for (Announcement a : list) {
            AdminAnnouncementVO vo = new AdminAnnouncementVO();
            BeanUtils.copyProperties(a, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional
    public void create(AnnouncementDTO dto) {
        Announcement a = new Announcement();
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setType(dto.getType() != null ? dto.getType() : 1);
        a.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : 0);
        a.setSort(dto.getSort() != null ? dto.getSort() : 0);
        a.setStatus(0);
        a.setCreateUserId(SecurityUtils.getCurrentUserId());
        announcementMapper.insert(a);
    }

    @Override
    @Transactional
    public void update(AnnouncementDTO dto) {
        Announcement a = announcementMapper.selectById(dto.getId());
        if (a == null) {
            throw new BusinessException("公告不存在");
        }

        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        if (dto.getType() != null) {
            a.setType(dto.getType());
        }
        if (dto.getIsTop() != null) {
            a.setIsTop(dto.getIsTop());
        }
        if (dto.getSort() != null) {
            a.setSort(dto.getSort());
        }
        announcementMapper.updateById(a);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("公告不存在");
        }
        a.setStatus(1);
        announcementMapper.updateById(a);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("公告不存在");
        }
        a.setStatus(0);
        announcementMapper.updateById(a);
    }
}
