package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import com.ljc.chaocommunity.service.UserSearchService;
import com.ljc.chaocommunity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/user")
@Tag(name = "用户管理（管理端）")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSearchService userSearchService;

    // ===== 用户管理 =====

    @GetMapping("/list")
    @Operation(summary = "分页查询用户")
    public Result<PageResult<UserVO>> listAll(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "15") int size) {
        return Result.success(userService.listAllUsers(page, size));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "根据ID查询用户详情")
    public Result<UserVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.adminGetUserDetail(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "ES搜索用户（含封禁用户）")
    public Result<PageResult<UserVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "排序：comprehensive(相关度) / followers(粉丝数)") @RequestParam(defaultValue = "comprehensive") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "15") int size) {
        return Result.success(userSearchService.search(keyword, true, sort, page, size));
    }

    @PutMapping("/{userId}/ban")
    @Operation(summary = "封禁/解封用户")
    public Result<Void> toggleBan(@PathVariable Long userId) {
        userService.toggleBanUser(userId);
        return Result.success();
    }

    // ===== 用户审核 =====

    @GetMapping("/apply/list")
    @Operation(summary = "分页查询审核列表")
    public Result<PageResult<UserApplyVO>> getApplyList(@RequestParam(required = false) Integer status,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "15") int size) {
        return Result.success(userService.getApplyList(status, page, size));
    }

    @PutMapping("/apply/{applyId}/approve")
    @Operation(summary = "审核通过")
    public Result<Void> approve(@PathVariable Long applyId) {
        userService.approveApply(applyId);
        return Result.success();
    }

    @PutMapping("/apply/{applyId}/reject")
    @Operation(summary = "审核驳回")
    public Result<Void> reject(@PathVariable Long applyId, @RequestBody Map<String, String> body) {
        userService.rejectApply(applyId, body.get("reason"));
        return Result.success();
    }

    @DeleteMapping("/apply/{applyId}")
    @Operation(summary = "删除审核记录")
    public Result<Void> deleteApply(@PathVariable Long applyId) {
        userService.deleteApply(applyId);
        return Result.success();
    }

    // ===== ES同步 =====

    @PostMapping("/es-sync")
    @Operation(summary = "全量同步用户到ES")
    public Result<Long> syncEs() {
        return Result.success(userSearchService.fullSync());
    }

    // ===== 用户资料管理 =====

    @PutMapping("/{userId}/reset-nickname")
    @Operation(summary = "重置用户昵称为随机名")
    public Result<String> resetNickname(@PathVariable Long userId) {
        return Result.success(userService.resetNickname(userId));
    }

    @PutMapping("/{userId}/clear-signature")
    @Operation(summary = "清空用户签名")
    public Result<Void> clearSignature(@PathVariable Long userId) {
        userService.clearSignature(userId);
        return Result.success();
    }

    @PutMapping("/{userId}/clear-avatar")
    @Operation(summary = "清空用户头像")
    public Result<Void> clearAvatar(@PathVariable Long userId) {
        userService.clearAvatar(userId);
        return Result.success();
    }
}
