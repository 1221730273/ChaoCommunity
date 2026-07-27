package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.CommentMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.pojo.dto.CommentDTO;
import com.ljc.chaocommunity.pojo.dto.CommentPageQueryDTO;
import com.ljc.chaocommunity.pojo.entity.Comment;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.CommentVO;
import com.ljc.chaocommunity.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostMapper postMapper;

    @Override
    public PageResult<CommentVO> pageQueryByPostId(CommentPageQueryDTO dto) {
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
        //TODO 引入springSecurity之后获取当前用户ID
        comment.setUserId(1L);
        comment.setPostId(dto.getPostId());
        comment.setContent(dto.getContent());
        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        commentMapper.insert(comment);

        // 4. 更新帖子的评论数
        Post updatePost = new Post();
        updatePost.setId(dto.getPostId());
        updatePost.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(updatePost);

        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> pageQueryByUserId(CommentPageQueryDTO dto) {
        //TODO 引入springSecurity之后获取当前用户ID
        Long currentUserId = 1L;
        Page<CommentVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<CommentVO> resultPage = commentMapper.selectPageVoByUserId(page, currentUserId, dto.getSort());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    @Override
    public void deleteComment(Long commentId) {
        // 1. 校验评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }

        // 2. 校验是不是自己的评论
        //TODO 引入springSecurity之后获取当前用户ID
        Long currentUserId = 1L;
        if (!comment.getUserId().equals(currentUserId)) {
            throw new BusinessException("只能删除自己的评论");
        }

        commentMapper.deleteById(commentId);
    }
}
