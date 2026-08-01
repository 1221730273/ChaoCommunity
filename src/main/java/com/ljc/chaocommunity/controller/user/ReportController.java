package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.dto.ReportDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report")
@Tag(name = "举报（用户端）")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @PostMapping
    @Operation(summary = "提交举报")
    public Result<Void> submitReport(@Valid @RequestBody ReportDTO dto) {
        reportService.submitReport(dto);
        return Result.success();
    }

    //TODO 后续可以把举报功能弄的更正式 比如举报理由 举报的图片
}
