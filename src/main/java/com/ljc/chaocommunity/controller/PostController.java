package com.ljc.chaocommunity.controller;


import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post")
@Tag(name = "帖子管理")
public class PostController {

    //TODO 以后post新增是否精选表或者字段 把精选帖子放到主页

    @Autowired
    private PostService postService;


    /**
     * 创建帖子
     */
    @PostMapping
    @Operation(summary = "创建帖子")
    public Result<Long> createPost(@Valid @RequestBody PostDTO dto) {

        Long postId = postService.createPost(dto);

        return Result.success(postId);
    }
    /**
     * 删除帖子
     */
    @DeleteMapping("/{postId}")
    @Operation(summary = "删除帖子")
    public Result<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return Result.success();
    }
    /**
     * 修改帖子
     */
    @PutMapping
    @Operation(summary = "修改帖子")
    public Result<Void> updatePost(@Valid @RequestBody PostDTO dto) {
        if (dto.getId()== null){
            return Result.error("帖子ID不能为空");
        }
        postService.updatePost(dto);
        return Result.success();
    }
    /**
     * 查询帖子详情
     */
    @GetMapping("/{postId}")
    @Operation(summary = "查询帖子详情")
    public Result<PostVO> getPost(@PathVariable Long postId) {
        PostVO vo = postService.getPostVOById(postId);
        return Result.success(vo);
    }
    /**
     * 分页查询帖子列表(根据最新最热)
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询帖子列表")
    public Result<PageResult<PostVO>> listPosts(@Valid PostPageQueryDTO dto) {
        PageResult<PostVO> result = postService.pageQuery(dto);
        return Result.success(result);
    }



    //TODO 以后可以新增一个功能:用户查询自己的评论 点击对应的评论跳转到对应的帖子然后定位到自己的评论（思路是修改根据id查询详细帖子的接口）

    //TODO 以后引入websocket 和 消息队列 新增评论推送 帖子更新推送

}

