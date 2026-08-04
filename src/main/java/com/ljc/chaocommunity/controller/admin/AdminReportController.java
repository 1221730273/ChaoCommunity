package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.dto.HandleReportDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.ReportVO;
import com.ljc.chaocommunity.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/report")
@Tag(name = "举报（管理端）")
public class AdminReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/list")
    @Operation(summary = "分页查询举报列表")
    public Result<PageResult<ReportVO>> list(@RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "15") int size) {
        return Result.success(reportService.getReportList(status, page, size));
    }

    @PutMapping("/handle")
    @Operation(summary = "处理举报")
    public Result<Void> handle(@Valid @RequestBody HandleReportDTO dto) {
        reportService.handleReport(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除举报记录")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.deleteReport(id);
        return Result.success();
    }
}
