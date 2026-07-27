package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class UserController {

    //TODO 用户可以有等级像b站那样 等级,经验


    @Autowired
    private PostService postService;

    @GetMapping("/{userId}/posts")
    @Operation(summary = "获取用户帖子列表")
    public Result<PageResult<PostVO>> getUserPosts(@PathVariable Long userId, PostPageQueryDTO dto) {
        return Result.success(postService.getUserPosts(userId, dto));
    }


    //TODO 后续给user数据库表增加是否封禁字段 配合springsecurity 实现用户封禁
}
