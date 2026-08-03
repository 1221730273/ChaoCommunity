package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.mapper.UserApplyMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.entity.UserApply;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import com.ljc.chaocommunity.service.UserService;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserApplyMapper userApplyMapper;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 用户端：提交修改申请 ====================

    @Override
    @Transactional
    public void updateProfile(UserProfileDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 检查是否有同类型待审核的申请
        LambdaQueryWrapper<UserApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApply::getUserId, currentUserId)
                .eq(UserApply::getType, "PROFILE")
                .eq(UserApply::getStatus, 0);
        if (userApplyMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已有资料修改申请在审核中");
        }

        UserApply apply = new UserApply();
        apply.setUserId(currentUserId);
        apply.setType("PROFILE");
        apply.setNickname(dto.getNickname());
        apply.setSignature(dto.getSignature());
        apply.setStatus(0);
        userApplyMapper.insert(apply);
    }

    @Override
    @Transactional
    public void updateAvatar(CoverUpdateDTO dto) {
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

        // 2. 检查是否有同类型待审核的申请
        LambdaQueryWrapper<UserApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserApply::getUserId, currentUserId)
                .eq(UserApply::getType, "AVATAR")
                .eq(UserApply::getStatus, 0);
        if (userApplyMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已有头像修改申请在审核中");
        }

        // 3. 写入 user_apply
        UserApply apply = new UserApply();
        apply.setUserId(currentUserId);
        apply.setType("AVATAR");
        apply.setAvatarFileId(dto.getFileId());
        apply.setStatus(0);
        userApplyMapper.insert(apply);
    }

    // ==================== 用户资料查询 ====================

    @Override
    public UserVO getMyProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserVO(user, true);
    }

    @Override
    public UserVO getUserProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserVO(user, false);
    }

    /**
     * User → UserVO
     * @param includeEmail 是否包含邮箱（仅自己可见）
     */
    private UserVO buildUserVO(User user, boolean includeEmail) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setSignature(user.getSignature());
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());

        if (includeEmail) {
            vo.setEmail(user.getEmail());
        }

        return vo;
    }

    // ==================== 管理端：用户管理 ====================

    @Override
    public List<UserVO> listAllUsers() {
        List<User> users = userMapper.selectList(null);
        List<UserVO> voList = new ArrayList<>();
        for (User user : users) {
            voList.add(buildUserVO(user, true));
        }
        return voList;
    }

    @Override
    public UserVO adminGetUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserVO(user, true);
    }

    @Override
    public List<UserVO> searchUsers(String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getUsername, keyword)
                .or()
                .like(User::getNickname, keyword)
                .orderByDesc(User::getCreateTime);
        List<User> users = userMapper.selectList(wrapper);
        List<UserVO> voList = new ArrayList<>();
        for (User user : users) {
            voList.add(buildUserVO(user, true));
        }
        return voList;
    }

    @Override
    @Transactional
    public void toggleBanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 不能封禁管理员
        if (user.getRole() == 1) {
            throw new BusinessException("不能封禁管理员");
        }

        // 切换封禁状态
        int newStatus = user.getStatus() == 1 ? 0 : 1;
        user.setStatus(newStatus);
        userMapper.updateById(user);

        // 封禁时踢掉该用户的登录token
        if (newStatus == 1) {
            String token = (String) redisTemplate.opsForValue().get("auth:user:" + userId + ":token");
            if (token != null) {
                redisTemplate.delete("auth:token:" + token);
                redisTemplate.delete("auth:user:" + userId + ":token");
            }
        }
    }

    // ==================== 管理端：审核 ====================

    @Override
    public List<UserApplyVO> getApplyList(Integer status) {
        LambdaQueryWrapper<UserApply> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(UserApply::getStatus, status);
        }
        wrapper.orderByDesc(UserApply::getCreateTime);
        List<UserApply> applies = userApplyMapper.selectList(wrapper);

        List<UserApplyVO> voList = new ArrayList<>();
        for (UserApply apply : applies) {
            User user = userMapper.selectById(apply.getUserId());
            if (user == null) continue;

            UserApplyVO vo = new UserApplyVO();
            vo.setId(apply.getId());
            vo.setUserId(apply.getUserId());
            vo.setUsername(user.getUsername());
            vo.setType(apply.getType());
            vo.setNickname(apply.getNickname());
            vo.setAvatarFileId(apply.getAvatarFileId());
            vo.setSignature(apply.getSignature());
            vo.setStatus(apply.getStatus());
            vo.setRejectReason(apply.getRejectReason());
            vo.setCreateTime(apply.getCreateTime());

            // 当前值（方便管理员对比）
            vo.setCurrentNickname(user.getNickname());
            vo.setCurrentAvatar(user.getAvatar());
            vo.setCurrentSignature(user.getSignature());

            // 如果是头像审核，查出新头像的 URL
            if (apply.getAvatarFileId() != null) {
                FileRecord fileRecord = fileRecordMapper.selectById(apply.getAvatarFileId());
                if (fileRecord != null) {
                    vo.setAvatarUrl(fileRecord.getUrl());
                }
            }

            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional
    public void approveApply(Long applyId) {
        Long handlerId = SecurityUtils.getCurrentUserId();

        UserApply apply = userApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException("申请不存在");
        }
        if (apply.getStatus() != 0) {
            throw new BusinessException("该申请已处理过");
        }

        User user = userMapper.selectById(apply.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        switch (apply.getType()) {
            case "PROFILE":
                if (apply.getNickname() != null) {
                    user.setNickname(apply.getNickname());
                }
                if (apply.getSignature() != null) {
                    user.setSignature(apply.getSignature());
                }
                break;

            case "AVATAR":
                // 1. 移动文件 temp/ → user/avatar/
                FileRecord newFile = fileRecordMapper.selectById(apply.getAvatarFileId());
                if (newFile == null) {
                    throw new BusinessException("文件不存在");
                }
                String oldObjectKey = newFile.getFilePath();
                String newObjectKey = oldObjectKey.replace("temp/", "user/avatar/");
                OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

                // 2. 新文件 status=1
                newFile.setFilePath(moveResult.objectKey());
                newFile.setUrl(moveResult.url());
                newFile.setStatus(1);
                fileRecordMapper.updateById(newFile);

                // 3. 释放旧头像
                if (user.getAvatarFileId() != null) {
                    FileRecord oldFile = fileRecordMapper.selectById(user.getAvatarFileId());
                    if (oldFile != null && oldFile.getStatus() == 1) {
                        oldFile.setStatus(0);
                        fileRecordMapper.updateById(oldFile);
                    }
                }

                // 4. 更新用户头像
                user.setAvatar(moveResult.url());
                user.setAvatarFileId(newFile.getId());
                break;

            default:
                throw new BusinessException("不支持的审核类型");
        }

        userMapper.updateById(user);

        // 更新申请状态
        apply.setStatus(1);
        apply.setHandlerId(handlerId);
        userApplyMapper.updateById(apply);
    }

    @Override
    @Transactional
    public void rejectApply(Long applyId, String reason) {
        Long handlerId = SecurityUtils.getCurrentUserId();

        UserApply apply = userApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException("申请不存在");
        }
        if (apply.getStatus() != 0) {
            throw new BusinessException("该申请已处理过");
        }

        apply.setStatus(2);
        apply.setRejectReason(reason);
        apply.setHandlerId(handlerId);
        userApplyMapper.updateById(apply);
    }
}
