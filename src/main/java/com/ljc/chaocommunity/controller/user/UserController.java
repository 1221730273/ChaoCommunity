package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.PostService;
import com.ljc.chaocommunity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class UserController {

    //TODO 用户可以有等级像b站那样 等级,经验


    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    @Operation(summary = "查看自己的完整资料")
    public Result<com.ljc.chaocommunity.pojo.vo.UserVO> getMyProfile() {
        return Result.success(userService.getMyProfile());
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "查看别人的资料（不含隐私字段）")
    public Result<com.ljc.chaocommunity.pojo.vo.UserVO> getUserProfile(@PathVariable Long userId) {
        return Result.success(userService.getUserProfile(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "修改用户资料（需审核）")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    @PutMapping("/avatar")
    @Operation(summary = "修改用户头像（需审核）")
    public Result<Void> updateAvatar(@Valid @RequestBody CoverUpdateDTO dto) {
        userService.updateAvatar(dto);
        return Result.success();
    }

    @GetMapping("/{userId}/posts")
    @Operation(summary = "获取用户帖子列表")
    public Result<PageResult<PostVO>> getUserPosts(@PathVariable Long userId, PostPageQueryDTO dto) {
        return Result.success(postService.getUserPosts(userId, dto));
    }




    //TODO 后续给user数据库表增加是否封禁字段 配合springsecurity 实现用户封禁


    //TODO 以后支持邮箱修改绑定现在不支持 以后支持接入qq登录
}
