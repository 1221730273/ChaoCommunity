package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.FeedbackVO;
import com.ljc.chaocommunity.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/feedback")
@Tag(name = "反馈（管理端）")
public class AdminFeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/list")
    @Operation(summary = "分页查询反馈列表")
    public Result<PageResult<FeedbackVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return Result.success(feedbackService.list(type, status, page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除反馈记录")
    public Result<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return Result.success();
    }
}
