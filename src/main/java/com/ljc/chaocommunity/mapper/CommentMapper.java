package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.pojo.entity.Comment;
import com.ljc.chaocommunity.pojo.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /** 根据帖子ID分页查询评论 */
    Page<CommentVO> selectPageVoByPostId(Page<CommentVO> page,
                                         @Param("postId") Long postId,
                                         @Param("sort") String sort);

    /** 根据用户ID分页查询评论 */
    Page<CommentVO> selectPageVoByUserId(Page<CommentVO> page,
                                         @Param("userId") Long userId,
                                         @Param("sort") String sort);

    /** 分页查询所有评论（管理端） */
    Page<CommentVO> selectPageVoAll(Page<CommentVO> page);
}
