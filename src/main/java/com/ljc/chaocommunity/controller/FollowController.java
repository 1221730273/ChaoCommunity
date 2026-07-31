package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.FollowCountVO;
import com.ljc.chaocommunity.pojo.vo.FollowVO;
import com.ljc.chaocommunity.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/following")
    @Operation(summary = "获取关注列表（关注了多少人）")
    public Result<List<FollowVO>> getFollowingList(@RequestParam(required = false) Long userId) {
        return Result.success(followService.getFollowingList(userId));
    }

    @GetMapping("/followers")
    @Operation(summary = "获取粉丝列表（多少人关注了TA）")
    public Result<List<FollowVO>> getFollowerList(@RequestParam(required = false) Long userId) {
        return Result.success(followService.getFollowerList(userId));
    }

    @GetMapping("/{userId}/count")
    @Operation(summary = "获取关注数和粉丝数")
    public Result<FollowCountVO> getFollowCount(@PathVariable Long userId) {
        return Result.success(followService.getFollowCount(userId));
    }
}
