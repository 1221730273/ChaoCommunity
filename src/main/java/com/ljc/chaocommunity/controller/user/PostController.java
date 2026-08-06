package com.ljc.chaocommunity.controller.user;


import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.PostSearchService;
import com.ljc.chaocommunity.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/post")
@Tag(name = "帖子管理")
public class PostController {


    @Autowired
    private PostService postService;

    @Autowired
    private PostSearchService postSearchService;


    /**
     * 创建帖子（提交审核）
     */
    @PostMapping
    @Operation(summary = "创建帖子（提交审核）")
    public Result<Long> createPost(@Valid @RequestBody PostDTO dto) {

        Long auditId = postService.createPost(dto);

        return Result.success(auditId);
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
     * 修改帖子内容（提交审核）
     */
    @PutMapping
    @Operation(summary = "修改帖子内容（提交审核）")
    public Result<Long> updatePost(@Valid @RequestBody PostDTO dto) {
        if (dto.getId() == null) {
            return Result.error("帖子ID不能为空");
        }
        Long auditId = postService.updatePost(dto);
        return Result.success(auditId);
    }

    /**
     * 修改帖子封面（提交审核）
     */
    @PutMapping("/{postId}/cover")
    @Operation(summary = "修改帖子封面（提交审核）")
    public Result<Long> updateCover(@PathVariable Long postId,
                                    @Valid @RequestBody CoverUpdateDTO dto) {
        Long auditId = postService.updateCover(postId, dto);
        return Result.success(auditId);
    }
    /**
     * 查询帖子详情
     */
    @GetMapping("/{postId}")
    @Operation(summary = "查询帖子详情")
    public Result<PostVO> getPost(@PathVariable Long postId) {
        postService.incrementViewCount(postId);
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

    /**
     * 分页查询精选帖子
     */
    @GetMapping("/featured")
    @Operation(summary = "查询精选帖子")
    public Result<PageResult<PostVO>> getFeaturedPosts(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(defaultValue = "newest") String sort) {
        return Result.success(postService.pageQueryFeatured(page, size, sort));
    }

    /**
     * 查询最新帖子（首页展示，默认 4 条）
     */
    @GetMapping("/latest")
    @Operation(summary = "查询最新帖子")
    public Result<List<PostVO>> getLatestPosts(@RequestParam(defaultValue = "4") int limit) {
        return Result.success(postService.getLatestPosts(limit));
    }

    /**
     * 查询最新精选帖子（首页展示，默认 2 条）
     */
    @GetMapping("/featured/latest")
    @Operation(summary = "查询最新精选帖子")
    public Result<List<PostVO>> getLatestFeatured(@RequestParam(defaultValue = "2") int limit) {
        return Result.success(postService.getLatestFeatured(limit));
    }

    /**
     * 根据用户ID查询帖子列表（查别人只查可见帖子，查自己包括隐藏帖子）
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询帖子列表")
    public Result<PageResult<PostVO>> getUserPosts(@PathVariable Long userId,
                                                    @Valid PostPageQueryDTO dto) {
        return Result.success(postService.getUserPosts(userId, dto));
    }

    /**
     * 切换帖子隐藏状态（0↔1），仅本人
     */
    @PutMapping("/{postId}/hide")
    @Operation(summary = "切换帖子隐藏状态")
    public Result<Void> toggleHidePost(@PathVariable Long postId) {
        postService.toggleHidePost(postId);
        return Result.success();
    }

    /**
     * 查询自己的帖子（包括隐藏的，仅从post表）
     */
    @GetMapping("/my")
    @Operation(summary = "查询自己的帖子（含隐藏）")
    public Result<PageResult<PostVO>> getMyPosts(@Valid PostPageQueryDTO dto) {
        return Result.success(postService.getMyPosts(dto));
    }

    /**
     * 查询自己的审核记录（新帖审核）
     */
    @GetMapping("/my/audits")
    @Operation(summary = "查询自己的审核记录")
    public Result<List<PostAuditVO>> getMyAudits() {
        return Result.success(postService.getMyAudits());
    }

    /**
     * 删除自己审核失败的记录
     */
    @DeleteMapping("/audit/{auditId}")
    @Operation(summary = "删除审核失败记录")
    public Result<Void> deleteFailedAudit(@PathVariable Long auditId) {
        postService.deleteFailedAudit(auditId);
        return Result.success();
    }

    /**
     * 搜索帖子（仅可见帖子）
     */
    @GetMapping("/search")
    @Operation(summary = "搜索帖子（关键词 + 分类 + 排序）")
    public Result<PageResult<PostVO>> search(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "排序：comprehensive(综合) / newest(最新) / hot(最热)") @RequestParam(defaultValue = "comprehensive") String sort,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return Result.success(postSearchService.search(keyword, categoryId, sort, false, page, size));
    }

}
