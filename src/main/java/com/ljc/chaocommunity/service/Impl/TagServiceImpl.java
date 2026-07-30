package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.mapper.TagMapper;
import com.ljc.chaocommunity.pojo.dto.TagDTO;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.entity.Tag;
import com.ljc.chaocommunity.service.TagService;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Override
    public List<Tag> listAll() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Tag::getCreateTime);
        return tagMapper.selectList(wrapper);
    }

    @Override
    public Tag getById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }
        return tag;
    }

    @Override
    @Transactional
    public void create(TagDTO dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());

        // 处理图标
        if (dto.getFileId() != null) {
            String iconUrl = moveIcon(dto.getFileId(), null);
            tag.setIcon(iconUrl);
        }

        tagMapper.insert(tag);
    }

    @Override
    @Transactional
    public void update(TagDTO dto) {
        Tag tag = tagMapper.selectById(dto.getId());
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        tag.setName(dto.getName());

        // 处理图标：传了新fileId才处理
        if (dto.getFileId() != null) {
            String iconUrl = moveIcon(dto.getFileId(), tag.getIcon());
            tag.setIcon(iconUrl);
        }

        tagMapper.updateById(tag);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException("标签不存在");
        }

        // 如果有图标，把对应file_record的status置为0
        if (tag.getIcon() != null) {
            LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileRecord::getUrl, tag.getIcon());
            FileRecord fileRecord = fileRecordMapper.selectOne(wrapper);
            if (fileRecord != null) {
                fileRecord.setStatus(0);
                fileRecordMapper.updateById(fileRecord);
            }
        }

        tagMapper.deleteById(id);
    }

    /**
     * 移动图标文件：temp/ → tag/icon/
     * @param fileId 新文件ID
     * @param oldIconUrl 旧图标URL（没有则传null）
     * @return 新图标URL
     */
    private String moveIcon(Long fileId, String oldIconUrl) {
        FileRecord fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new BusinessException("文件不存在");
        }
        if (!fileRecord.getUserId().equals(SecurityUtils.getCurrentUserId())) {
            throw new BusinessException("无权使用该文件");
        }
        if (fileRecord.getStatus() == 1) {
            throw new BusinessException("文件已经被使用");
        }
        if (!fileRecord.getFilePath().startsWith("temp/")) {
            throw new BusinessException("非法文件");
        }

        // 把旧图标的file_record的status置为0
        if (oldIconUrl != null) {
            LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileRecord::getUrl, oldIconUrl);
            FileRecord oldFileRecord = fileRecordMapper.selectOne(wrapper);
            if (oldFileRecord != null) {
                oldFileRecord.setStatus(0);
                fileRecordMapper.updateById(oldFileRecord);
            }
        }

        // 移动文件
        String oldObjectKey = fileRecord.getFilePath();
        String newObjectKey = oldObjectKey.replace("temp/", "tag/icon/");
        OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

        // 更新file_record
        fileRecord.setFilePath(moveResult.objectKey());
        fileRecord.setUrl(moveResult.url());
        fileRecord.setStatus(1);
        fileRecordMapper.updateById(fileRecord);

        return moveResult.url();
    }
}
