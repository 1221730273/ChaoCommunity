package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.dto.RejectAuditDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.PostAuditService;
import com.ljc.chaocommunity.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/post")
@Tag(name = "帖子管理（管理端）")
public class AdminPostController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostAuditService postAuditService;

    // ===== 帖子管理 =====

    @GetMapping("/list")
    @Operation(summary = "分页查询所有帖子（包含隐藏）")
    public Result<PageResult<PostVO>> listAll(@Valid PostPageQueryDTO dto) {
        return Result.success(postService.pageQueryAll(dto));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询帖子（包含隐藏）")
    public Result<PageResult<PostVO>> getUserPosts(@PathVariable Long userId,
                                                    @Valid PostPageQueryDTO dto) {
        return Result.success(postService.getUserPosts(userId, dto));
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "删除帖子（不限本人）")
    public Result<Void> deletePost(@PathVariable Long postId) {
        postService.adminDeletePost(postId);
        return Result.success();
    }

    @PutMapping("/{postId}/featured")
    @Operation(summary = "设置/取消精选")
    public Result<Void> toggleFeatured(@PathVariable Long postId) {
        postService.toggleFeatured(postId);
        return Result.success();
    }

    // ===== 审核管理 =====

    @GetMapping("/audit/list")
    @Operation(summary = "分页查询审核列表")
    public Result<PageResult<PostAuditVO>> auditList(@RequestParam(required = false) Integer status,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "15") int size) {
        return Result.success(postAuditService.getAuditList(status, page, size));
    }

    @GetMapping("/audit/user/{userId}")
    @Operation(summary = "根据用户ID查询审核记录")
    public Result<List<PostAuditVO>> userAuditList(@PathVariable Long userId) {
        return Result.success(postAuditService.getAuditListByUserId(userId));
    }

    @PutMapping("/audit/{id}/approve")
    @Operation(summary = "审核通过")
    public Result<Void> approve(@PathVariable Long id) {
        postAuditService.approveAudit(id);
        return Result.success();
    }

    @PutMapping("/audit/reject")
    @Operation(summary = "审核拒绝")
    public Result<Void> reject(@Valid @RequestBody RejectAuditDTO dto) {
        postAuditService.rejectAudit(dto.getId(), dto.getReason());
        return Result.success();
    }

    @DeleteMapping("/audit/{id}")
    @Operation(summary = "删除审核记录")
    public Result<Void> deleteAudit(@PathVariable Long id) {
        postAuditService.deleteAudit(id);
        return Result.success();
    }
}
