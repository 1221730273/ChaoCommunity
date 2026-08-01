package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import com.ljc.chaocommunity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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

    // ===== 用户管理 =====

    @GetMapping("/list")
    @Operation(summary = "查询所有用户")
    public Result<List<UserVO>> listAll() {
        return Result.success(userService.listAllUsers());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "根据ID查询用户详情")
    public Result<UserVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.adminGetUserDetail(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "根据用户名/昵称搜索用户")
    public Result<List<UserVO>> search(@RequestParam String keyword) {
        return Result.success(userService.searchUsers(keyword));
    }

    @PutMapping("/{userId}/ban")
    @Operation(summary = "封禁/解封用户")
    public Result<Void> toggleBan(@PathVariable Long userId) {
        userService.toggleBanUser(userId);
        return Result.success();
    }

    // ===== 用户审核 =====

    @GetMapping("/apply/list")
    @Operation(summary = "查询审核列表")
    public Result<List<UserApplyVO>> getApplyList(@RequestParam(required = false) Integer status) {
        return Result.success(userService.getApplyList(status));
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
}
