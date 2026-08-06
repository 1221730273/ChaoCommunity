package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.service.PostSearchService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostSearchService postSearchService;

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

        // 2. 如果是回复评论，校验父评论是否存在
        if (dto.getParentId() != null && dto.getParentId() != 0) {
            Long count = commentMapper.selectCount(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getId, dto.getParentId())
            );
            if (count == 0) {
                throw new BusinessException("父评论不存在");
            }
        }

        // 3. 保存评论
        Comment comment = new Comment();
        comment.setUserId(SecurityUtils.getCurrentUserId());
        comment.setPostId(dto.getPostId());
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        commentMapper.insert(comment);

        // 4. 更新帖子的评论数
        int newCount = post.getCommentCount() + 1;
        Post updatePost = new Post();
        updatePost.setId(dto.getPostId());
        updatePost.setCommentCount(newCount);
        postMapper.updateById(updatePost);
        // ES 同步
        postSearchService.updateCommentCount(dto.getPostId(), newCount);

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

        // 4. 更新帖子评论数 -1
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            int newCount = post.getCommentCount() - 1;
            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setCommentCount(newCount);
            postMapper.updateById(updatePost);
            // ES 同步
            postSearchService.updateCommentCount(post.getId(), newCount);
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

        // 更新帖子评论数 -1
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            int newCount = post.getCommentCount() - 1;
            Post updatePost = new Post();
            updatePost.setId(post.getId());
            updatePost.setCommentCount(newCount);
            postMapper.updateById(updatePost);
            // ES 同步
            postSearchService.updateCommentCount(post.getId(), newCount);
        }
    }


    @Override
    public CommentContextVO getCommentContext(Long commentId) {
        // 1. 查询目标评论
        CommentVO target = commentMapper.selectVoById(commentId);
        if (target == null) {
            throw new BusinessException("评论不存在");
        }

        if (target.getParentId() == 0) {
            // 目标是一级评论：返回该评论 + 所有子回复
            List<CommentVO> children = commentMapper.selectVoByParentId(target.getId());
            return CommentContextVO.builder()
                    .targetType("ROOT")
                    .targetId(target.getId())
                    .rootComment(target)
                    .children(children)
                    .build();
        } else {
            // 目标是二级回复：返回父评论 + 所有兄弟回复
            CommentVO parent = commentMapper.selectVoById(target.getParentId());
            if (parent == null) {
                throw new BusinessException("父评论不存在");
            }
            List<CommentVO> children = commentMapper.selectVoByParentId(parent.getId());
            return CommentContextVO.builder()
                    .targetType("CHILD")
                    .targetId(target.getId())
                    .rootComment(parent)
                    .children(children)
                    .build();
        }
    }

}
