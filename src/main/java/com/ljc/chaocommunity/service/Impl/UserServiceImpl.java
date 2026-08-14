package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.mapper.UserApplyMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.entity.UserApply;
import com.ljc.chaocommunity.pojo.redis.UserCache;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import com.ljc.chaocommunity.mq.EsSyncProducer;
import com.ljc.chaocommunity.service.UserService;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private EsSyncProducer esSyncProducer;

    /** 用户详情缓存 key 前缀 */
    private static final String USER_DETAIL_CACHE_KEY = "user:detail:";
    /** 关注数计数 key 前缀 */
    private static final String USER_FOLLOW_COUNT_KEY = "user:followCnt:";
    /** 粉丝数计数 key 前缀 */
    private static final String USER_FOLLOWER_COUNT_KEY = "user:followerCnt:";
    /** 详情缓存 TTL：30 分钟 */
    private static final long USER_DETAIL_CACHE_TTL = 30;
    /** 防穿透空缓存 TTL：30 秒 */
    private static final long USER_EMPTY_CACHE_TTL = 30;
    /** 计数缓存 TTL：30 分钟 */
    private static final long USER_COUNT_CACHE_TTL = 30;

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

    // ==================== 用户资料查询（带 Redis 缓存） ====================

    @Override
    public UserVO getMyProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return getProfileWithCache(currentUserId);
    }

    @Override
    public UserVO getUserProfile(Long userId) {
        return getProfileWithCache(userId);
    }

    /**
     * 获取用户资料（带缓存）：
     * 1. 先查 Redis `user:detail:{id}`，命中直接返回
     * 2. 未命中查数据库，写入缓存（30 分钟）
     * 3. 用户不存在时缓存空对象（30 秒）防穿透
     * 关注数、粉丝数不缓存，从 Redis 计数 key 实时读（DB 兜底）
     */
    private UserVO getProfileWithCache(Long userId) {
        String key = USER_DETAIL_CACHE_KEY + userId;

        // 1. 先查 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof UserCache) {
            UserCache cache = (UserCache) cached;
            if (cache.getId() == null) {
                throw new BusinessException("用户不存在");
            }
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(cache, vo);
            // 关注数、粉丝数不缓存，从 Redis 计数 key 读，DB 兜底
            User dbUser = userMapper.selectById(userId);
            int dbFollow = dbUser != null ? dbUser.getFollowCount() : 0;
            int dbFollower = dbUser != null ? dbUser.getFollowerCount() : 0;
            vo.setFollowCount(readCount(USER_FOLLOW_COUNT_KEY, userId, dbFollow));
            vo.setFollowerCount(readCount(USER_FOLLOWER_COUNT_KEY, userId, dbFollower));
            return vo;
        }

        // 2. 缓存未命中，查数据库
        User user = userMapper.selectById(userId);
        if (user == null) {
            // 3. 用户不存在：缓存空对象 30 秒防穿透
            redisTemplate.opsForValue().set(key, new UserCache(), USER_EMPTY_CACHE_TTL, TimeUnit.SECONDS);
            throw new BusinessException("用户不存在");
        }

        // 写入缓存（剔除关注数、粉丝数）
        UserCache cache = new UserCache();
        BeanUtils.copyProperties(user, cache);
        redisTemplate.opsForValue().set(key, cache, USER_DETAIL_CACHE_TTL, TimeUnit.MINUTES);

        // 初始化两个计数 key（仅当 key 不存在时写入 DB 值，避免覆盖实时增量）
        initCountIfAbsent(USER_FOLLOW_COUNT_KEY, userId, user.getFollowCount());
        initCountIfAbsent(USER_FOLLOWER_COUNT_KEY, userId, user.getFollowerCount());

        return buildUserVO(user);
    }

    /**
     * User → UserVO（不含邮箱，邮箱仅注册输入，不对外返回）
     */
    private UserVO buildUserVO(User user) {
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
        return vo;
    }

    /**
     * 删除用户缓存：详情缓存 + 关注/粉丝两个计数 key（资料/头像变更、封禁时调用）
     */
    private void evictUserCache(Long userId) {
        redisTemplate.delete(USER_DETAIL_CACHE_KEY + userId);
        redisTemplate.delete(USER_FOLLOW_COUNT_KEY + userId);
        redisTemplate.delete(USER_FOLLOWER_COUNT_KEY + userId);
    }

    /**
     * 计数 key 不存在时用 DB 值兜底初始化（setIfAbsent，避免覆盖已有实时增量），TTL 30 分钟
     */
    private void initCountIfAbsent(String prefix, Long userId, Integer dbValue) {
        if (dbValue != null) {
            redisTemplate.opsForValue().setIfAbsent(prefix + userId, dbValue, USER_COUNT_CACHE_TTL, TimeUnit.MINUTES);
        }
    }

    /**
     * 从 Redis 计数 key 读取；key 不存在时回退 DB 值并初始化
     */
    private int readCount(String prefix, Long userId, int dbFallback) {
        Object val = redisTemplate.opsForValue().get(prefix + userId);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        initCountIfAbsent(prefix, userId, dbFallback);
        return dbFallback;
    }

    // ==================== 管理端：用户管理 ====================

    @Override
    public PageResult<UserVO> listAllUsers(int page, int size) {
        Page<User> p = new Page<>(page, size);
        Page<User> resultPage = userMapper.selectPage(p, new LambdaQueryWrapper<User>().orderByDesc(User::getCreateTime));
        List<UserVO> voList = resultPage.getRecords().stream()
                .map(this::buildUserVO)
                .collect(Collectors.toList());
        return new PageResult<>(resultPage.getTotal(), voList);
    }

    @Override
    public UserVO adminGetUserDetail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return buildUserVO(user);
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
            voList.add(buildUserVO(user));
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

        // ES 同步：更新封禁状态（异步发消息）
        esSyncProducer.sendUserUpdateStatus(userId, newStatus);
        // 清理 Redis：详情缓存 + 计数 key（封禁状态变化，缓存需失效）
        evictUserCache(userId);

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
    public PageResult<UserApplyVO> getApplyList(Integer status, int page, int size) {
        LambdaQueryWrapper<UserApply> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(UserApply::getStatus, status);
        }
        wrapper.orderByDesc(UserApply::getCreateTime);

        Page<UserApply> p = new Page<>(page, size);
        Page<UserApply> resultPage = userApplyMapper.selectPage(p, wrapper);

        List<UserApplyVO> voList = new ArrayList<>();
        for (UserApply apply : resultPage.getRecords()) {
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

            vo.setCurrentNickname(user.getNickname());
            vo.setCurrentAvatar(user.getAvatar());
            vo.setCurrentSignature(user.getSignature());

            if (apply.getAvatarFileId() != null) {
                FileRecord fileRecord = fileRecordMapper.selectById(apply.getAvatarFileId());
                if (fileRecord != null) {
                    vo.setAvatarUrl(fileRecord.getUrl());
                }
            }

            voList.add(vo);
        }
        return new PageResult<>(resultPage.getTotal(), voList);
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
        // ES 同步：资料/头像变更
        esSyncProducer.sendUserIndex(user);
        // 清理 Redis：详情缓存 + 关注/粉丝计数 key
        evictUserCache(user.getId());

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

    @Override
    public void deleteApply(Long applyId) {
        UserApply apply = userApplyMapper.selectById(applyId);
        if (apply == null) throw new BusinessException("审核记录不存在");
        userApplyMapper.deleteById(applyId);
    }

    // ==================== 管理端：用户资料管理 ====================

    @Override
    public String resetNickname(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        String newNickname = "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user.setNickname(newNickname);
        userMapper.updateById(user);
        esSyncProducer.sendUserIndex(user);
        // 清理 Redis 缓存
        evictUserCache(userId);
        return newNickname;
    }

    @Override
    public void clearSignature(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId).set(User::getSignature, null);
        userMapper.update(null, uw);
        // ES 同步
        User updated = userMapper.selectById(userId);
        if (updated != null) esSyncProducer.sendUserIndex(updated);
        // 清理 Redis 缓存
        evictUserCache(userId);
    }

    @Override
    @Transactional
    public void clearAvatar(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        // 释放旧头像文件
        if (user.getAvatarFileId() != null) {
            FileRecord fr = fileRecordMapper.selectById(user.getAvatarFileId());
            if (fr != null && fr.getStatus() == 1) {
                fr.setStatus(0);
                fileRecordMapper.updateById(fr);
            }
        }
        LambdaUpdateWrapper<User> uw = new LambdaUpdateWrapper<>();
        uw.eq(User::getId, userId)
                .set(User::getAvatar, null)
                .set(User::getAvatarFileId, null);
        userMapper.update(null, uw);
        // ES 同步
        User updated = userMapper.selectById(userId);
        if (updated != null) esSyncProducer.sendUserIndex(updated);
        // 清理 Redis 缓存
        evictUserCache(userId);
    }
}
