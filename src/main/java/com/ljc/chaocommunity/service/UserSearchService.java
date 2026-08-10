package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.UserVO;

/**
 * Elasticsearch 用户搜索服务
 */
public interface UserSearchService {

    /** 写入或覆盖一个用户到 ES（注册/资料变更后调用） */
    void index(User user);

    /** 更新用户封禁状态 */
    void updateStatus(Long userId, Integer status);

    /**
     * 搜索用户，直接从 ES 返回数据
     * @param keyword       搜索关键词
     * @param includeBanned 是否包含封禁用户
     * @param sort          comprehensive(相关度) / followers(粉丝数)
     */
    PageResult<UserVO> search(String keyword, boolean includeBanned, String sort, int page, int size);

    /** 全量同步所有未删除用户到 ES */
    long fullSync();
}
