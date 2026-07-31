package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
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
