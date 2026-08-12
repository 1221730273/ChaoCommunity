package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorite")
@Tag(name = "收藏管理")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/post/{postId}")
    @Operation(summary = "收藏帖子")
    public Result<Void> favorite(@PathVariable Long postId) {
        favoriteService.favorite(postId);
        return Result.success();
    }

    @DeleteMapping("/post/{postId}")
    @Operation(summary = "取消收藏")
    public Result<Void> unfavorite(@PathVariable Long postId) {
        favoriteService.unfavorite(postId);
        return Result.success();
    }

    @GetMapping("/post/{postId}/status")
    @Operation(summary = "查询当前用户是否已收藏帖子")
    public Result<Map<String, Boolean>> isFavorited(@PathVariable Long postId) {
        return Result.success(Map.of("favorited", favoriteService.isFavorited(postId)));
    }

    @PostMapping("/post/status/batch")
    @Operation(summary = "批量查询帖子收藏状态")
    public Result<Map<Long, Boolean>> isFavoritedBatch(@RequestBody List<Long> postIds) {
        return Result.success(favoriteService.isFavoritedBatch(postIds));
    }

    @GetMapping("/my")
    @Operation(summary = "分页获取我的收藏")
    public Result<PageResult<PostVO>> myFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(favoriteService.myFavorites(page, size));
    }
}
