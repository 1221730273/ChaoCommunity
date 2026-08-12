package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.FollowVO;
import com.ljc.chaocommunity.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follow")
@Tag(name = "关注管理")
public class FollowController {

    @Autowired
    private FollowService followService;

    @PostMapping("/{userId}")
    @Operation(summary = "关注用户")
    public Result<Void> follow(@PathVariable Long userId) {
        followService.follow(userId);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "取消关注")
    public Result<Void> unfollow(@PathVariable Long userId) {
        followService.unfollow(userId);
        return Result.success();
    }

    @GetMapping("/{userId}/status")
    @Operation(summary = "查询是否已关注")
    public Result<Boolean> isFollowing(@PathVariable Long userId) {
        return Result.success(followService.isFollowing(userId));
    }

    @PostMapping("/status/batch")
    @Operation(summary = "批量查询关注状态")
    public Result<java.util.Map<Long, Boolean>> isFollowingBatch(@RequestBody java.util.List<Long> userIds) {
        return Result.success(followService.isFollowingBatch(userIds));
    }

    // ==================== 查自己（需登录）====================

    @GetMapping("/me/following")
    @Operation(summary = "获取自己的关注列表")
    public Result<PageResult<FollowVO>> getMyFollowing(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getMyFollowing(page, size));
    }

    @GetMapping("/me/followers")
    @Operation(summary = "获取自己的粉丝列表")
    public Result<PageResult<FollowVO>> getMyFollowers(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getMyFollowers(page, size));
    }

    // ==================== 查别人（公开）====================

    @GetMapping("/{userId}/following")
    @Operation(summary = "获取指定用户的关注列表")
    public Result<PageResult<FollowVO>> getUserFollowing(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getUserFollowing(userId, page, size));
    }

    @GetMapping("/{userId}/followers")
    @Operation(summary = "获取指定用户的粉丝列表")
    public Result<PageResult<FollowVO>> getUserFollowers(@PathVariable Long userId,
                                                          @RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size) {
        return Result.success(followService.getUserFollowers(userId, page, size));
    }
}
