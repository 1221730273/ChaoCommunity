package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;

public interface FavoriteService {

    /** 收藏帖子 */
    void favorite(Long postId);

    /** 取消收藏 */
    void unfavorite(Long postId);

    /** 查询当前用户是否已收藏帖子 */
    boolean isFavorited(Long postId);

    /** 批量查询当前用户对帖子的收藏状态 */
    java.util.Map<Long, Boolean> isFavoritedBatch(java.util.List<Long> postIds);

    /** 分页获取我的收藏 */
    PageResult<PostVO> myFavorites(int page, int size);
}
