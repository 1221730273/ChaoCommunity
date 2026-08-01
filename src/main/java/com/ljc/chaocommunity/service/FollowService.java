package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.FollowCountVO;
import com.ljc.chaocommunity.pojo.vo.FollowVO;

/**
 * 关注服务
 */
public interface FollowService {

    /** 关注用户 */
    void follow(Long followeeId);

    /** 取消关注 */
    void unfollow(Long followeeId);

    /** 是否已关注 */
    boolean isFollowing(Long userId);

    /** 批量查询是否已关注 */
    java.util.Map<Long, Boolean> isFollowingBatch(java.util.List<Long> userIds);

    // ===== 查自己（需登录）=====

    /** 获取自己的关注列表 */
    PageResult<FollowVO> getMyFollowing(int page, int size);

    /** 获取自己的粉丝列表 */
    PageResult<FollowVO> getMyFollowers(int page, int size);

    // ===== 查别人（公开）=====

    /** 获取指定用户的关注列表 */
    PageResult<FollowVO> getUserFollowing(Long userId, int page, int size);

    /** 获取指定用户的粉丝列表 */
    PageResult<FollowVO> getUserFollowers(Long userId, int page, int size);

    /** 获取指定用户的关注数和粉丝数 */
    FollowCountVO getFollowCount(Long userId);
}
