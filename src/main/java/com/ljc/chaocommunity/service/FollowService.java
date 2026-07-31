package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.vo.FollowCountVO;
import com.ljc.chaocommunity.pojo.vo.FollowVO;

import java.util.List;

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

    /** 获取关注列表（关注了多少人） */
    List<FollowVO> getFollowingList(Long userId);

    /** 获取粉丝列表（多少人关注了TA） */
    List<FollowVO> getFollowerList(Long userId);

    /** 获取关注数和粉丝数 */
    FollowCountVO getFollowCount(Long userId);
}
