package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.dto.CommentDTO;
import com.ljc.chaocommunity.pojo.dto.CommentPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.CommentVO;
import com.ljc.chaocommunity.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
@Tag(name = "评论管理")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 根据帖子ID分页查询评论
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询评论")
    public Result<PageResult<CommentVO>> listComments(CommentPageQueryDTO dto) {
        PageResult<CommentVO> result = commentService.pageQueryByPostId(dto);
        return Result.success(result);
    }

    /**
     * 创建评论
     */
    @PostMapping
    @Operation(summary = "创建评论")
    public Result<Long> createComment(@Valid @RequestBody CommentDTO dto) {
        return Result.success(commentService.createComment(dto));
    }

    /**
     * 查询当前用户的评论
     */
    @GetMapping("/my")
    @Operation(summary = "查询我的评论")
    public Result<PageResult<CommentVO>> listMyComments(CommentPageQueryDTO dto) {
        PageResult<CommentVO> result = commentService.pageQueryByUserId(dto);
        return Result.success(result);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return Result.success();
    }
}
