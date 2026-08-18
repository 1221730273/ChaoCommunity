package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.PrivateConversationMapper;
import com.ljc.chaocommunity.mapper.UserFollowMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.mq.NotifyProducer;
import com.ljc.chaocommunity.pojo.entity.PrivateConversation;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.entity.UserFollow;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.FollowVO;
import com.ljc.chaocommunity.service.FollowService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotifyProducer notifyProducer;

    @Autowired
    private PrivateConversationMapper privateConversationMapper;

    /** 关注数计数 key 前缀 */
    private static final String USER_FOLLOW_COUNT_KEY = "user:followCnt:";
    /** 粉丝数计数 key 前缀 */
    private static final String USER_FOLLOWER_COUNT_KEY = "user:followerCnt:";
    /** 计数缓存 TTL：30 分钟 */
    private static final long USER_COUNT_CACHE_TTL = 30;

    @Override
    @Transactional
    public void follow(Long followeeId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(followeeId)) {
            throw new BusinessException("不能关注自己");
        }

        User followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw new BusinessException("用户不存在");
        }

        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFolloweeId, followeeId);
        if (userFollowMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已关注该用户");
        }

        UserFollow uf = new UserFollow();
        uf.setFollowerId(currentUserId);
        uf.setFolloweeId(followeeId);
        userFollowMapper.insert(uf);

        // DB 原子自增：关注者 follow_count +1
        LambdaUpdateWrapper<User> myUw = new LambdaUpdateWrapper<>();
        myUw.eq(User::getId, currentUserId)
                .setSql("follow_count = follow_count + 1");
        userMapper.update(null, myUw);
        // DB 原子自增：被关注者 follower_count +1
        LambdaUpdateWrapper<User> targetUw = new LambdaUpdateWrapper<>();
        targetUw.eq(User::getId, followeeId)
                .setSql("follower_count = follower_count + 1");
        userMapper.update(null, targetUw);

        // Redis 计数 +1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
        User follower = userMapper.selectById(currentUserId);
        if (follower != null) {
            redisTemplate.opsForValue().setIfAbsent(USER_FOLLOW_COUNT_KEY + currentUserId, follower.getFollowCount(), USER_COUNT_CACHE_TTL, TimeUnit.MINUTES);
        }
        redisTemplate.opsForValue().setIfAbsent(USER_FOLLOWER_COUNT_KEY + followeeId, followee.getFollowerCount(), USER_COUNT_CACHE_TTL, TimeUnit.MINUTES);
        redisTemplate.opsForValue().increment(USER_FOLLOW_COUNT_KEY + currentUserId);
        redisTemplate.opsForValue().increment(USER_FOLLOWER_COUNT_KEY + followeeId);

        // 通知：有人关注了你（已禁止关注自己，无需再判）
        notifyProducer.sendFollow(followeeId, SecurityUtils.getLoginUser().getUser());

        // 关注解锁私信：对方曾给我发私信等待回应时，关注后直接解锁聊天（status 0→1）
        LambdaQueryWrapper<PrivateConversation> pendingCw = new LambdaQueryWrapper<>();
        pendingCw.eq(PrivateConversation::getUser1Id, followeeId)
                .eq(PrivateConversation::getUser2Id, currentUserId)
                .eq(PrivateConversation::getStatus, 0);
        List<PrivateConversation> pendings = privateConversationMapper.selectList(pendingCw);
        for (PrivateConversation pc : pendings) {
            pc.setStatus(1);
            privateConversationMapper.updateById(pc); // updateTime 自动填充
        }
    }

    @Override
    @Transactional
    public void unfollow(Long followeeId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        if (currentUserId.equals(followeeId)) {
            throw new BusinessException("不能对自己操作");
        }

        User followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw new BusinessException("用户不存在");
        }

        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFolloweeId, followeeId);
        int deleted = userFollowMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException("未关注该用户");
        }

        // DB 原子自减（GREATEST 保证不为负）：关注者 follow_count -1
        LambdaUpdateWrapper<User> myUw = new LambdaUpdateWrapper<>();
        myUw.eq(User::getId, currentUserId)
                .setSql("follow_count = GREATEST(COALESCE(follow_count,0) - 1, 0)");
        userMapper.update(null, myUw);
        // DB 原子自减：被关注者 follower_count -1
        LambdaUpdateWrapper<User> targetUw = new LambdaUpdateWrapper<>();
        targetUw.eq(User::getId, followeeId)
                .setSql("follower_count = GREATEST(COALESCE(follower_count,0) - 1, 0)");
        userMapper.update(null, targetUw);

        // Redis 计数 -1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
        User follower = userMapper.selectById(currentUserId);
        if (follower != null) {
            redisTemplate.opsForValue().setIfAbsent(USER_FOLLOW_COUNT_KEY + currentUserId, follower.getFollowCount(), USER_COUNT_CACHE_TTL, TimeUnit.MINUTES);
        }
        redisTemplate.opsForValue().setIfAbsent(USER_FOLLOWER_COUNT_KEY + followeeId, followee.getFollowerCount(), USER_COUNT_CACHE_TTL, TimeUnit.MINUTES);
        redisTemplate.opsForValue().decrement(USER_FOLLOW_COUNT_KEY + currentUserId);
        redisTemplate.opsForValue().decrement(USER_FOLLOWER_COUNT_KEY + followeeId);
    }

    @Override
    public boolean isFollowing(Long userId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId.equals(userId)) {
            return false;
        }

        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFolloweeId, userId);
        return userFollowMapper.selectCount(wrapper) > 0;
    }

    @Override
    public java.util.Map<Long, Boolean> isFollowingBatch(java.util.List<Long> userIds) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        java.util.Map<Long, Boolean> result = new java.util.HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, currentUserId)
                .in(UserFollow::getFolloweeId, userIds);
        java.util.Set<Long> followedIds = userFollowMapper.selectList(wrapper).stream()
                .map(UserFollow::getFolloweeId)
                .collect(java.util.stream.Collectors.toSet());
        for (Long userId : userIds) {
            if (currentUserId.equals(userId)) {
                result.put(userId, false);
            } else {
                result.put(userId, followedIds.contains(userId));
            }
        }
        return result;
    }

    // ==================== 查自己（需登录）====================

    @Override
    public PageResult<FollowVO> getMyFollowing(int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return queryFollowingList(currentUserId, page, size);
    }

    @Override
    public PageResult<FollowVO> getMyFollowers(int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return queryFollowerList(currentUserId, page, size);
    }

    // ==================== 查别人（公开）====================

    @Override
    public PageResult<FollowVO> getUserFollowing(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return queryFollowingList(userId, page, size);
    }

    @Override
    public PageResult<FollowVO> getUserFollowers(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return queryFollowerList(userId, page, size);
    }

    // ==================== 私有方法 ====================

    /** 分页查询某个用户的关注列表 */
    private PageResult<FollowVO> queryFollowingList(Long userId, int page, int size) {
        Page<UserFollow> p = new Page<>(page, size);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreateTime);
        Page<UserFollow> result = userFollowMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getTotal(), buildFollowVOList(result.getRecords(), "followee"));
    }

    /** 分页查询某个用户的粉丝列表 */
    private PageResult<FollowVO> queryFollowerList(Long userId, int page, int size) {
        Page<UserFollow> p = new Page<>(page, size);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFolloweeId, userId)
                .orderByDesc(UserFollow::getCreateTime);
        Page<UserFollow> result = userFollowMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getTotal(), buildFollowVOList(result.getRecords(), "follower"));
    }

    /**
     * 根据关注记录构建 FollowVO 列表
     */
    private List<FollowVO> buildFollowVOList(List<UserFollow> follows, String type) {
        List<FollowVO> voList = new ArrayList<>();
        for (UserFollow uf : follows) {
            Long targetUserId = "followee".equals(type) ? uf.getFolloweeId() : uf.getFollowerId();
            User user = userMapper.selectById(targetUserId);
            if (user == null) continue;

            FollowVO vo = new FollowVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setSignature(user.getSignature());
            vo.setCreateTime(uf.getCreateTime());
            voList.add(vo);
        }
        return voList;
    }
}
