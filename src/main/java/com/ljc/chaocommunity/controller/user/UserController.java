package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import com.ljc.chaocommunity.service.PostService;
import com.ljc.chaocommunity.service.UserSearchService;
import com.ljc.chaocommunity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Tag(name = "用户接口")
public class UserController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserSearchService userSearchService;

    @GetMapping("/me")
    @Operation(summary = "查看自己的完整资料")
    public Result<UserVO> getMyProfile() {
        return Result.success(userService.getMyProfile());
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "查看别人的资料（不含隐私字段）")
    public Result<UserVO> getUserProfile(@PathVariable Long userId) {
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

    @GetMapping("/search")
    @Operation(summary = "搜索用户（仅正常用户）")
    public Result<PageResult<UserVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "排序：comprehensive(相关度) / followers(粉丝数)") @RequestParam(defaultValue = "comprehensive") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return Result.success(userSearchService.search(keyword, false, sort, page, size));
    }

}
