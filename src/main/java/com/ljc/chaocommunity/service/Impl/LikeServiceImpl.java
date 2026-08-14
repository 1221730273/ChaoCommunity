package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ljc.chaocommunity.mq.EsSyncProducer;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

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

    @Autowired
    private EsSyncProducer esSyncProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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

        //判断有没有点过赞
        if (postLikeMapper.selectCount(wrapper) > 0) {
            return;
        }

        PostLike postLike = new PostLike();
        postLike.setUserId(currentUserId);
        postLike.setPostId(postId);
        postLikeMapper.insert(postLike);

        // DB 原子自增（like_count = like_count + 1），避免并发覆盖丢失
        LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
        uw.eq(Post::getId, postId)
                .setSql("like_count = like_count + 1");
        postMapper.update(null, uw);
        // ES 同步（异步发消息，script 原子增减）
        esSyncProducer.sendPostUpdateLikeCount(postId, 1);
        // Redis 点赞计数 +1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
        redisTemplate.opsForValue().setIfAbsent("post:likeCnt:" + postId, post.getLikeCount(), 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().increment("post:likeCnt:" + postId);
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

        // DB 原子自减（GREATEST 保证不为负），避免并发覆盖丢失
        LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
        uw.eq(Post::getId, postId)
                .setSql("like_count = GREATEST(COALESCE(like_count,0) - 1, 0)");
        postMapper.update(null, uw);
        // ES 同步（异步发消息，script 原子增减）
        esSyncProducer.sendPostUpdateLikeCount(postId, -1);
        // Redis 点赞计数 -1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
        redisTemplate.opsForValue().setIfAbsent("post:likeCnt:" + postId, post.getLikeCount(), 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().decrement("post:likeCnt:" + postId);
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

        // DB 原子自增（like_count = like_count + 1），避免并发覆盖丢失
        LambdaUpdateWrapper<Comment> uw = new LambdaUpdateWrapper<>();
        uw.eq(Comment::getId, commentId)
                .setSql("like_count = like_count + 1");
        commentMapper.update(null, uw);
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

        // DB 原子自减（GREATEST 保证不为负），避免并发覆盖丢失
        LambdaUpdateWrapper<Comment> uw = new LambdaUpdateWrapper<>();
        uw.eq(Comment::getId, commentId)
                .setSql("like_count = GREATEST(COALESCE(like_count,0) - 1, 0)");
        commentMapper.update(null, uw);
    }

    @Override
    public boolean isCommentLiked(Long commentId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, currentUserId)
                .eq(CommentLike::getCommentId, commentId);
        return commentLikeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public java.util.Map<Long, Boolean> isPostLikedBatch(java.util.List<Long> postIds) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        java.util.Map<Long, Boolean> result = new java.util.HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getUserId, currentUserId)
                .in(PostLike::getPostId, postIds);
        java.util.Set<Long> likedIds = postLikeMapper.selectList(wrapper).stream()
                .map(PostLike::getPostId)
                .collect(java.util.stream.Collectors.toSet());
        for (Long postId : postIds) {
            result.put(postId, likedIds.contains(postId));
        }
        return result;
    }

    @Override
    public java.util.Map<Long, Boolean> isCommentLikedBatch(java.util.List<Long> commentIds) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        java.util.Map<Long, Boolean> result = new java.util.HashMap<>();
        if (commentIds == null || commentIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, currentUserId)
                .in(CommentLike::getCommentId, commentIds);
        java.util.Set<Long> likedIds = commentLikeMapper.selectList(wrapper).stream()
                .map(CommentLike::getCommentId)
                .collect(java.util.stream.Collectors.toSet());
        for (Long commentId : commentIds) {
            result.put(commentId, likedIds.contains(commentId));
        }
        return result;
    }
}
