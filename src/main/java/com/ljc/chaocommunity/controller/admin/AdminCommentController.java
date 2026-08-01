package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.CommentVO;
import com.ljc.chaocommunity.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/comment")
@Tag(name = "评论管理（管理端）")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/list")
    @Operation(summary = "分页查询所有评论")
    public Result<PageResult<CommentVO>> listAll(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.success(commentService.pageQueryAll(page, size));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "删除评论（不限本人）")
    public Result<Void> delete(@PathVariable Long commentId) {
        commentService.adminDeleteComment(commentId);
        return Result.success();
    }
}
