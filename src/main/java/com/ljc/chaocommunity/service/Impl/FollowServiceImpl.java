package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.UserFollowMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.entity.UserFollow;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.FollowCountVO;
import com.ljc.chaocommunity.pojo.vo.FollowVO;
import com.ljc.chaocommunity.service.FollowService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FollowServiceImpl implements FollowService {

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private UserMapper userMapper;

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

        User follower = userMapper.selectById(currentUserId);
        follower.setFollowCount(follower.getFollowCount() + 1);
        userMapper.updateById(follower);

        followee.setFollowerCount(followee.getFollowerCount() + 1);
        userMapper.updateById(followee);
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

        User follower = userMapper.selectById(currentUserId);
        follower.setFollowCount(Math.max(0, follower.getFollowCount() - 1));
        userMapper.updateById(follower);

        followee.setFollowerCount(Math.max(0, followee.getFollowerCount() - 1));
        userMapper.updateById(followee);
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

    @Override
    public FollowCountVO getFollowCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new FollowCountVO(user.getFollowCount(), user.getFollowerCount());
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
