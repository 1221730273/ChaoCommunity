package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.service.UserService;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Override
    @Transactional
    public void updateProfile(UserProfileDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }

        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateAvatar(CoverUpdateDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);

        // 1. 查询新文件
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

        // 2. 查询旧头像fileId
        Long oldFileId = null;
        if (user.getAvatar() != null) {
            LambdaQueryWrapper<FileRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileRecord::getUrl, user.getAvatar());
            FileRecord oldFileRecord = fileRecordMapper.selectOne(wrapper);
            if (oldFileRecord != null) {
                oldFileId = oldFileRecord.getId();
            }
        }

        // 3. 新旧fileId一致则直接返回
        if (oldFileId != null && oldFileId.equals(dto.getFileId())) {
            return;
        }

        // 4. 移动新文件到正式目录
        String oldObjectKey = newFileRecord.getFilePath();
        String newObjectKey = oldObjectKey.replace("temp/", "avatar/");
        OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

        // 5. 旧头像status置0
        if (oldFileId != null) {
            FileRecord oldFileRecord = fileRecordMapper.selectById(oldFileId);
            if (oldFileRecord != null) {
                oldFileRecord.setStatus(0);
                fileRecordMapper.updateById(oldFileRecord);
            }
        }

        // 6. 新头像status置1
        newFileRecord.setFilePath(moveResult.objectKey());
        newFileRecord.setUrl(moveResult.url());
        newFileRecord.setBizType("avatar");
        newFileRecord.setStatus(1);
        fileRecordMapper.updateById(newFileRecord);

        // 7. 更新用户头像
        user.setAvatar(moveResult.url());
        userMapper.updateById(user);
    }
}
