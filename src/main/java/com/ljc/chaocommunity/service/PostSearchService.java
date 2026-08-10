package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;

/**
 * Elasticsearch 帖子搜索服务接口
 */
public interface PostSearchService {

    /** 写入或覆盖一篇帖子到 ES */
    void index(Post post);

    /** 从 ES 中删除一篇帖子 */
    void delete(Long postId);

    /** 更新帖子状态（隐藏/公开） */
    void updateStatus(Long postId, Integer status);

    /** 更新置顶状态 */
    void updateTop(Long postId, Integer isTop);

    /** 更新点赞数（delta 增量，正负皆可，script 原子增减） */
    void updateLikeCount(Long postId, int delta);

    /** 更新浏览数（delta 增量，正负皆可，script 原子增减） */
    void updateViewCount(Long postId, int delta);

    /** 更新评论数（delta 增量，正负皆可，script 原子增减） */
    void updateCommentCount(Long postId, int delta);

    /** 批量隐藏某用户的所有帖子 */
    void batchHideByUserId(Long userId);

    /**
     * 全文搜索
     * @param keyword       搜索关键词（null 时查询全部）
     * @param categoryId    分类过滤（null 时不限）
     * @param sort          newest / hot
     * @param includeHidden 是否包含隐藏帖子
     */
    PageResult<PostVO> search(String keyword, Long categoryId, String sort, boolean includeHidden, int page, int size);

    /** 全量同步：将 DB 中所有 visible 帖子写入 ES */
    long fullSync();
}
