package com.ljc.chaocommunity.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.mapper.CommentLikeMapper;
import com.ljc.chaocommunity.mapper.PostLikeMapper;
import com.ljc.chaocommunity.pojo.entity.CommentLike;
import com.ljc.chaocommunity.pojo.entity.PostLike;
import com.ljc.chaocommunity.pojo.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/like")
@Tag(name = "点赞管理（管理端）")
public class AdminLikeController {

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @GetMapping("/user/{userId}/posts")
    @Operation(summary = "查询用户点赞的帖子ID列表")
    public Result<List<Long>> getUserLikedPosts(@PathVariable Long userId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getUserId, userId)
                .orderByDesc(PostLike::getCreateTime);
        List<Long> postIds = postLikeMapper.selectList(wrapper).stream()
                .map(PostLike::getPostId)
                .collect(Collectors.toList());
        return Result.success(postIds);
    }

    @GetMapping("/user/{userId}/comments")
    @Operation(summary = "查询用户点赞的评论ID列表")
    public Result<List<Long>> getUserLikedComments(@PathVariable Long userId) {
        LambdaQueryWrapper<CommentLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommentLike::getUserId, userId)
                .orderByDesc(CommentLike::getCreateTime);
        List<Long> commentIds = commentLikeMapper.selectList(wrapper).stream()
                .map(CommentLike::getCommentId)
                .collect(Collectors.toList());
        return Result.success(commentIds);
    }
}
