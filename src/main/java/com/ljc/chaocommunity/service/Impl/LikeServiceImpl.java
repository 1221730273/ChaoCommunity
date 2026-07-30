package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.CommentLikeMapper;
import com.ljc.chaocommunity.mapper.CommentMapper;
import com.ljc.chaocommunity.mapper.PostLikeMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.pojo.entity.Comment;
import com.ljc.chaocommunity.pojo.entity.CommentLike;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.PostLike;
import com.ljc.chaocommunity.service.LikeService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private CommentMapper commentMapper;

    // ==================== 帖子点赞 ====================

    @Override
    @Transactional
    public void likePost(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getUserId, currentUserId)
                .eq(PostLike::getPostId, postId);
        if (postLikeMapper.selectCount(wrapper) > 0) {
            return;
        }

        PostLike postLike = new PostLike();
        postLike.setUserId(currentUserId);
        postLike.setPostId(postId);
        postLikeMapper.insert(postLike);

        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setLikeCount(post.getLikeCount() + 1);
        postMapper.updateById(updatePost);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getUserId, currentUserId)
                .eq(PostLike::getPostId, postId);
        PostLike postLike = postLikeMapper.selectOne(wrapper);
        if (postLike == null) {
            return;
        }

        postLikeMapper.deleteById(postLike.getId());

        Post updatePost = new Post();
        updatePost.setId(postId);
        updatePost.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postMapper.updateById(updatePost);
    }

    @Override
    public boolean isPostLiked(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getUserId, currentUserId)
                .eq(PostLike::getPostId, postId);
        return postLikeMapper.selectCount(wrapper) > 0;
    }

    // ==================== 评论点赞 ====================

    @Override
    @Transactional
    public void likeComment(Long commentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 判断是否已点赞
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, currentUserId)
                .eq(CommentLike::getCommentId, commentId);
        if (commentLikeMapper.selectCount(wrapper) > 0) {
            return;
        }

        CommentLike commentLike = new CommentLike();
        commentLike.setUserId(currentUserId);
        commentLike.setCommentId(commentId);
        commentLikeMapper.insert(commentLike);

        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(updateComment);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, currentUserId)
                .eq(CommentLike::getCommentId, commentId);
        CommentLike commentLike = commentLikeMapper.selectOne(wrapper);
        if (commentLike == null) {
            return;
        }

        commentLikeMapper.deleteById(commentLike.getId());

        Comment updateComment = new Comment();
        updateComment.setId(commentId);
        updateComment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        commentMapper.updateById(updateComment);
    }

    @Override
    public boolean isCommentLiked(Long commentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, currentUserId)
                .eq(CommentLike::getCommentId, commentId);
        return commentLikeMapper.selectCount(wrapper) > 0;
    }
}
