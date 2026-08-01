package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.dto.AnnouncementDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.AdminAnnouncementVO;
import com.ljc.chaocommunity.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/announcement")
@Tag(name = "公告管理（管理端）")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/list")
    @Operation(summary = "查询所有公告（含下架）")
    public Result<List<AdminAnnouncementVO>> list() {
        return Result.success(announcementService.adminList());
    }

    @PostMapping
    @Operation(summary = "新增公告")
    public Result<Void> create(@Valid @RequestBody AnnouncementDTO dto) {
        announcementService.create(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改公告")
    public Result<Void> update(@Valid @RequestBody AnnouncementDTO dto) {
        if (dto.getId() == null) {
            return Result.error("公告ID不能为空");
        }
        announcementService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "下架公告")
    public Result<Void> delete(@PathVariable Long id) {
        announcementService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "上架公告")
    public Result<Void> publish(@PathVariable Long id) {
        announcementService.publish(id);
        return Result.success();
    }
}
