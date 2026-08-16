package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.mq.EsSyncProducer;
import com.ljc.chaocommunity.mq.NotifyProducer;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.CommentMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.pojo.dto.CommentDTO;
import com.ljc.chaocommunity.pojo.dto.CommentPageQueryDTO;
import com.ljc.chaocommunity.pojo.entity.Comment;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.CommentContextVO;
import com.ljc.chaocommunity.pojo.vo.CommentVO;
import com.ljc.chaocommunity.service.CommentService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private EsSyncProducer esSyncProducer;

    @Autowired
    private NotifyProducer notifyProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult<CommentVO> pageQueryByPostId(CommentPageQueryDTO dto) {
        // 隐藏帖子只有作者和管理员能看评论
        Post post = postMapper.selectById(dto.getPostId());
        if (post != null && post.getStatus() != 0) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (!post.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
                return new PageResult<>(0, java.util.Collections.emptyList());
            }
        }
        Page<CommentVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<CommentVO> resultPage = commentMapper.selectPageVoByPostId(page, dto.getPostId(), dto.getSort());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    @Transactional
    public Long createComment(CommentDTO dto) {
        // 1. 校验帖子是否存在
        Post post = postMapper.selectById(dto.getPostId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // 隐藏帖子不允许评论
        if (post.getStatus() != null && post.getStatus() != 0) {
            throw new BusinessException("帖子已隐藏，无法评论");
        }

        // 2. 如果是回复评论，校验父评论是否存在并取出（用于发通知）
        Comment parentComment = null;
        if (dto.getParentId() != null && dto.getParentId() != 0) {
            parentComment = commentMapper.selectById(dto.getParentId());
            if (parentComment == null) {
                throw new BusinessException("父评论不存在");
            }
        }

        // 3. 保存评论
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Comment comment = new Comment();
        comment.setUserId(currentUserId);
        comment.setPostId(dto.getPostId());
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        commentMapper.insert(comment);

        // 4. 更新帖子的评论数（DB 原子自增，避免并发覆盖丢失）
        LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
        uw.eq(Post::getId, dto.getPostId())
                .setSql("comment_count = comment_count + 1");
        postMapper.update(null, uw);
        // ES 同步（异步发消息，script 原子增减）
        esSyncProducer.sendPostUpdateCommentCount(dto.getPostId(), 1);
        // Redis 评论计数 +1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
        redisTemplate.opsForValue().setIfAbsent("post:commentCnt:" + dto.getPostId(), post.getCommentCount(), 30, TimeUnit.MINUTES);
        redisTemplate.opsForValue().increment("post:commentCnt:" + dto.getPostId());

        // 5. 通知：回复帖子（一级评论）通知帖子作者，回复评论通知父评论作者（自己回复自己不发）
        if (parentComment == null) {
            if (!post.getUserId().equals(currentUserId)) {
                notifyProducer.sendReplyPost(post.getUserId(), SecurityUtils.getLoginUser().getUser(), post, comment.getId(), dto.getContent());
            }
        } else if (!parentComment.getUserId().equals(currentUserId)) {
            notifyProducer.sendReplyComment(parentComment.getUserId(), SecurityUtils.getLoginUser().getUser(), post, parentComment, comment.getId(), dto.getContent());
        }

        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> pageQueryByUserId(CommentPageQueryDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<CommentVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<CommentVO> resultPage = commentMapper.selectPageVoByUserId(page, currentUserId, dto.getSort());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        // 1. 校验评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 2. 允许删除：①自己的评论 ②自己帖子下的评论 ③管理员
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isOwner = comment.getUserId().equals(currentUserId);
        boolean isPostAuthor = false;
        if (!isOwner) {
            Post post = postMapper.selectById(comment.getPostId());
            isPostAuthor = post != null && post.getUserId().equals(currentUserId);
        }
        if (!isOwner && !isPostAuthor && !SecurityUtils.isAdmin()) {
            throw new BusinessException("只能删除自己的评论");
        }

        // 3. 软删除评论
        commentMapper.deleteById(commentId);

        // 4. 更新帖子评论数 -1（DB 原子自减，GREATEST 保证不为负）
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
            uw.eq(Post::getId, post.getId())
                    .setSql("comment_count = GREATEST(COALESCE(comment_count,0) - 1, 0)");
            postMapper.update(null, uw);
            // ES 同步（异步发消息，script 原子增减）
            esSyncProducer.sendPostUpdateCommentCount(post.getId(), -1);
            // Redis 评论计数 -1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
            redisTemplate.opsForValue().setIfAbsent("post:commentCnt:" + post.getId(), post.getCommentCount(), 30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().decrement("post:commentCnt:" + post.getId());
        }
    }

    @Override
    public PageResult<CommentVO> pageQueryAll(int page, int size) {
        Page<CommentVO> p = new Page<>(page, size);
        Page<CommentVO> resultPage = commentMapper.selectPageVoAll(p);
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    @Transactional
    public void adminDeleteComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        commentMapper.deleteById(commentId);

        // 更新帖子评论数 -1（DB 原子自减，GREATEST 保证不为负）
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            LambdaUpdateWrapper<Post> uw = new LambdaUpdateWrapper<>();
            uw.eq(Post::getId, post.getId())
                    .setSql("comment_count = GREATEST(COALESCE(comment_count,0) - 1, 0)");
            postMapper.update(null, uw);
            // ES 同步（异步发消息，script 原子增减）
            esSyncProducer.sendPostUpdateCommentCount(post.getId(), -1);
            // Redis 评论计数 -1（key 不存在时用 DB 值兜底初始化，TTL 30 分钟）
            redisTemplate.opsForValue().setIfAbsent("post:commentCnt:" + post.getId(), post.getCommentCount(), 30, TimeUnit.MINUTES);
            redisTemplate.opsForValue().decrement("post:commentCnt:" + post.getId());
        }
    }


    @Override
    public CommentContextVO getCommentContext(Long commentId) {
        // 1. 查询目标评论
        CommentVO target = commentMapper.selectVoById(commentId);
        if (target == null) {
            throw new BusinessException("评论不存在");
        }

        // 2. 沿 parentId 向上收集完整祖先链：根 → ... → 目标（含目标自身）
        //    无论评论嵌套多深，都返回整条链，保证置顶区能展示完整父级结构
        List<CommentVO> chain = new ArrayList<>();
        CommentVO cur = target;
        while (cur != null) {
            chain.add(0, cur);
            Long parentId = cur.getParentId();
            if (parentId == null || parentId == 0) {
                break;
            }
            cur = commentMapper.selectVoById(parentId);
        }

        // 3. 目标评论的直接子回复（时间升序）
        List<CommentVO> children = commentMapper.selectVoByParentId(target.getId());

        boolean isRoot = target.getParentId() == null || target.getParentId() == 0;
        return CommentContextVO.builder()
                .targetType(isRoot ? "ROOT" : "CHILD")
                .targetId(target.getId())
                .rootComment(chain.get(0))
                .chain(chain)
                .children(children)
                .build();
    }

}
