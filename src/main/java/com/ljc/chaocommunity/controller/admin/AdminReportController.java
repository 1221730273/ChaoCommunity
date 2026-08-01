package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.dto.HandleReportDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.ReportVO;
import com.ljc.chaocommunity.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/report")
@Tag(name = "举报（管理端）")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/list")
    @Operation(summary = "查询举报列表")
    public Result<List<ReportVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(reportService.getReportList(status));
    }

    @PutMapping("/handle")
    @Operation(summary = "处理举报")
    public Result<Void> handle(@Valid @RequestBody HandleReportDTO dto) {
        reportService.handleReport(dto);
        return Result.success();
    }
}
