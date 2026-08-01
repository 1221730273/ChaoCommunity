package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.FollowVO;
import com.ljc.chaocommunity.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/follow")
@Tag(name = "关注管理（管理端）")
public class AdminFollowController {

    @Autowired
    private FollowService followService;

    @GetMapping("/{userId}/following")
    @Operation(summary = "查询用户的关注列表")
    public Result<PageResult<FollowVO>> getUserFollowing(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getUserFollowing(userId, page, size));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "查询用户的粉丝列表")
    public Result<PageResult<FollowVO>> getUserFollowers(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getUserFollowers(userId, page, size));
    }
}
