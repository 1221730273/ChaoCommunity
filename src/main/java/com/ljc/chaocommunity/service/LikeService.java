package com.ljc.chaocommunity.service;

public interface LikeService {

    // ===== 帖子点赞 =====

    /** 点赞帖子 */
    void likePost(Long postId);

    /** 取消点赞帖子 */
    void unlikePost(Long postId);

    /** 查询当前用户是否已点赞帖子 */
    boolean isPostLiked(Long postId);

    // ===== 评论点赞 =====

    /** 点赞评论 */
    void likeComment(Long commentId);

    /** 取消点赞评论 */
    void unlikeComment(Long commentId);

    /** 查询当前用户是否已点赞评论 */
    boolean isCommentLiked(Long commentId);

    /** 批量查询当前用户对帖子的点赞状态 */
    java.util.Map<Long, Boolean> isPostLikedBatch(java.util.List<Long> postIds);

    /** 批量查询当前用户对评论的点赞状态 */
    java.util.Map<Long, Boolean> isCommentLikedBatch(java.util.List<Long> commentIds);
}
