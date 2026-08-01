package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CommentDTO;
import com.ljc.chaocommunity.pojo.dto.CommentPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.CommentVO;

public interface CommentService {

    /** 根据帖子ID分页查询评论 */
    PageResult<CommentVO> pageQueryByPostId(CommentPageQueryDTO dto);

    /** 创建评论 */
    Long createComment(CommentDTO dto);

    /** 删除评论 */
    void deleteComment(Long commentId);

    /** 根据用户ID分页查询评论 */
    PageResult<CommentVO> pageQueryByUserId(CommentPageQueryDTO dto);

    /** 分页查询所有评论（管理端） */
    PageResult<CommentVO> pageQueryAll(int page, int size);

    /** 删除评论（管理端，不限本人） */
    void adminDeleteComment(Long commentId);
}
