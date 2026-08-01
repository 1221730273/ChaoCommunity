package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/like")
@Tag(name = "点赞管理")
public class LikeController {

    @Autowired
    private LikeService likeService;

    // ==================== 帖子点赞 ====================

    @PostMapping("/post/{postId}")
    @Operation(summary = "点赞帖子")
    public Result<Void> likePost(@PathVariable Long postId) {
        likeService.likePost(postId);
        return Result.success();
    }

    @DeleteMapping("/post/{postId}")
    @Operation(summary = "取消点赞帖子")
    public Result<Void> unlikePost(@PathVariable Long postId) {
        likeService.unlikePost(postId);
        return Result.success();
    }

    @GetMapping("/post/{postId}/status")
    @Operation(summary = "查询当前用户是否已点赞帖子")
    public Result<Map<String, Boolean>> isPostLiked(@PathVariable Long postId) {
        return Result.success(Map.of("liked", likeService.isPostLiked(postId)));
    }

    @PostMapping("/post/status/batch")
    @Operation(summary = "批量查询帖子点赞状态")
    public Result<Map<Long, Boolean>> isPostLikedBatch(@RequestBody List<Long> postIds) {
        return Result.success(likeService.isPostLikedBatch(postIds));
    }

    // ==================== 评论点赞 ====================

    @PostMapping("/comment/{commentId}")
    @Operation(summary = "点赞评论")
    public Result<Void> likeComment(@PathVariable Long commentId) {
        likeService.likeComment(commentId);
        return Result.success();
    }

    @DeleteMapping("/comment/{commentId}")
    @Operation(summary = "取消点赞评论")
    public Result<Void> unlikeComment(@PathVariable Long commentId) {
        likeService.unlikeComment(commentId);
        return Result.success();
    }

    @GetMapping("/comment/{commentId}/status")
    @Operation(summary = "查询当前用户是否已点赞评论")
    public Result<Map<String, Boolean>> isCommentLiked(@PathVariable Long commentId) {
        return Result.success(Map.of("liked", likeService.isCommentLiked(commentId)));
    }

    @PostMapping("/comment/status/batch")
    @Operation(summary = "批量查询评论点赞状态")
    public Result<Map<Long, Boolean>> isCommentLikedBatch(@RequestBody List<Long> commentIds) {
        return Result.success(likeService.isCommentLikedBatch(commentIds));
    }
}
