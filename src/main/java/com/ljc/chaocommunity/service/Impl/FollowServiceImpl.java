package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.UserFollowMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.entity.UserFollow;
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

        // 校验被关注者存在
        User followee = userMapper.selectById(followeeId);
        if (followee == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查是否已关注（利用唯一键 uk_follower_followee）
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFolloweeId, followeeId);
        if (userFollowMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("已关注该用户");
        }

        // 插入关注记录
        UserFollow uf = new UserFollow();
        uf.setFollowerId(currentUserId);
        uf.setFolloweeId(followeeId);
        userFollowMapper.insert(uf);

        // 更新冗余计数：关注者 follow_count +1
        User follower = userMapper.selectById(currentUserId);
        follower.setFollowCount(follower.getFollowCount() + 1);
        userMapper.updateById(follower);

        // 被关注者 follower_count +1
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

        // 删除关注记录
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFolloweeId, followeeId);
        int deleted = userFollowMapper.delete(wrapper);
        if (deleted == 0) {
            throw new BusinessException("未关注该用户");
        }

        // 更新冗余计数：关注者 follow_count -1
        User follower = userMapper.selectById(currentUserId);
        follower.setFollowCount(Math.max(0, follower.getFollowCount() - 1));
        userMapper.updateById(follower);

        // 被关注者 follower_count -1
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
    public List<FollowVO> getFollowingList(Long userId) {
        if (userId == null) {
            userId = SecurityUtils.getCurrentUserId();
        }

        // 查询该用户关注的记录，按时间倒序
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowerId, userId)
                .orderByDesc(UserFollow::getCreateTime);
        List<UserFollow> follows = userFollowMapper.selectList(wrapper);

        return buildFollowVOList(follows, "followee");
    }

    @Override
    public List<FollowVO> getFollowerList(Long userId) {
        if (userId == null) {
            userId = SecurityUtils.getCurrentUserId();
        }

        // 查询关注该用户的记录（粉丝），按时间倒序
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFolloweeId, userId)
                .orderByDesc(UserFollow::getCreateTime);
        List<UserFollow> follows = userFollowMapper.selectList(wrapper);

        return buildFollowVOList(follows, "follower");
    }

    @Override
    public FollowCountVO getFollowCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return new FollowCountVO(user.getFollowCount(), user.getFollowerCount());
    }

    /**
     * 根据关注记录构建 FollowVO 列表
     * @param follows 关注记录列表
     * @param type "followee"=查询的是关注的人 / "follower"=查询的是粉丝
     */
    private List<FollowVO> buildFollowVOList(List<UserFollow> follows, String type) {
        List<FollowVO> voList = new ArrayList<>();
        for (UserFollow uf : follows) {
            // 根据 type 决定查哪个人
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
