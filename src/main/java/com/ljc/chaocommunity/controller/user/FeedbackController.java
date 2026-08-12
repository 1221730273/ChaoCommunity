package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.dto.FeedbackDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedback")
@Tag(name = "反馈（用户端）")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "提交反馈")
    public Result<Void> submit(@Valid @RequestBody FeedbackDTO dto) {
        feedbackService.submit(dto);
        return Result.success();
    }
}
