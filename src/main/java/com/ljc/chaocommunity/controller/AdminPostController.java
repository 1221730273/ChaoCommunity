package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.dto.RejectAuditDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.service.PostAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/post")
@Tag(name = "帖子审核（管理端）")
public class AdminPostController {

    @Autowired
    private PostAuditService postAuditService;

    @GetMapping("/audit/list")
    @Operation(summary = "查询帖子审核列表")
    public Result<List<PostAuditVO>> list(@RequestParam(required = false) Integer status) {
        return Result.success(postAuditService.getAuditList(status));
    }

    @PutMapping("/audit/{id}/approve")
    @Operation(summary = "审核通过")
    public Result<Void> approve(@PathVariable Long id) {
        postAuditService.approveAudit(id);
        return Result.success();
    }

    @PutMapping("/audit/reject")
    @Operation(summary = "审核拒绝")
    public Result<Void> reject(@Valid @RequestBody RejectAuditDTO dto) {
        postAuditService.rejectAudit(dto.getId(), dto.getReason());
        return Result.success();
    }
}
