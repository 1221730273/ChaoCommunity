package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.dto.RejectAuditDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.PostAuditService;
import com.ljc.chaocommunity.service.PostSearchService;
import com.ljc.chaocommunity.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/post")
@Tag(name = "帖子管理（管理端）")
public class AdminPostController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostAuditService postAuditService;

    @Autowired
    private PostSearchService postSearchService;

    // ===== 帖子管理 =====

    @GetMapping("/list")
    @Operation(summary = "分页查询所有帖子（包含隐藏）")
    public Result<PageResult<PostVO>> listAll(@Valid PostPageQueryDTO dto) {
        return Result.success(postService.pageQueryAll(dto));
    }

    /**
     * 管理员搜索帖子（含隐藏帖子）
     */
    @GetMapping("/search")
    @Operation(summary = "管理员搜索帖子（含隐藏帖子）")
    public Result<PageResult<PostVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "排序：comprehensive(综合) / newest(最新) / hot(最热)") @RequestParam(defaultValue = "comprehensive") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "15") int size) {
        return Result.success(postSearchService.search(keyword, categoryId, sort, true, page, size));
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

    // ===== 置顶管理 =====

    @PutMapping("/{postId}/top")
    @Operation(summary = "设置/取消置顶")
    public Result<Void> toggleTop(@PathVariable Long postId) {
        postService.toggleTop(postId);
        return Result.success();
    }

    // ===== 隐藏管理 =====

    @PutMapping("/{postId}/hide")
    @Operation(summary = "管理员隐藏/公开帖子")
    public Result<Void> toggleHide(@PathVariable Long postId) {
        postService.toggleHidePost(postId);
        return Result.success();
    }

    @PutMapping("/user/{userId}/hide-all")
    @Operation(summary = "隐藏用户的所有帖子")
    public Result<Map<String, Integer>> hideUserPosts(@PathVariable Long userId) {
        int rows = postService.adminHideUserPosts(userId);
        return Result.success(Map.of("affected", rows));
    }

    // ===== ES同步 =====

    @PostMapping("/es-sync")
    @Operation(summary = "全量同步帖子到ES")
    public Result<Long> syncEs() {
        return Result.success(postSearchService.fullSync());
    }
}
