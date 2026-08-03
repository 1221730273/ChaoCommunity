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
            String iconUrl = moveIcon(dto.getFileId());
            tag.setIcon(iconUrl);
            tag.setFileId(dto.getFileId());
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

        // 处理图标：传了新 fileId 才更换
        if (dto.getFileId() != null) {
            // 1. 释放旧图标的 FileRecord
            releaseFileRecord(tag.getFileId());

            // 2. 移动新文件
            String iconUrl = moveIcon(dto.getFileId());
            tag.setIcon(iconUrl);
            tag.setFileId(dto.getFileId());
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

        // 释放图标对应的 FileRecord
        releaseFileRecord(tag.getFileId());

        tagMapper.deleteById(id);
    }

    // ==================== private ====================

    /**
     * 移动文件 temp/ → tag/icon/，更新 FileRecord status=1，返回新 URL
     */
    private String moveIcon(Long fileId) {
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

        String oldObjectKey = fileRecord.getFilePath();
        String newObjectKey = oldObjectKey.replace("temp/", "tag/icon/");
        OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

        fileRecord.setFilePath(moveResult.objectKey());
        fileRecord.setUrl(moveResult.url());
        fileRecord.setStatus(1);
        fileRecordMapper.updateById(fileRecord);

        return moveResult.url();
    }

    /**
     * 释放 FileRecord：status 置为 0
     */
    private void releaseFileRecord(Long fileId) {
        if (fileId == null) return;
        FileRecord fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord != null && fileRecord.getStatus() == 1) {
            fileRecord.setStatus(0);
            fileRecordMapper.updateById(fileRecord);
        }
    }
}
