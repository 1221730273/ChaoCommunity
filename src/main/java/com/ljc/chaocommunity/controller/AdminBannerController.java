package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.dto.UpdateBannerDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.AdminBannerVO;
import com.ljc.chaocommunity.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/banner")
@Tag(name = "轮播图（管理端）")
public class AdminBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    @Operation(summary = "查询所有轮播图（含关闭的）")
    public Result<List<AdminBannerVO>> list() {
        return Result.success(bannerService.getAdminBanners());
    }

    @PostMapping
    @Operation(summary = "新增轮播图")
    public Result<Void> create(@Valid @RequestBody UpdateBannerDTO dto) {
        bannerService.create(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新轮播图")
    public Result<Void> update(@Valid @RequestBody UpdateBannerDTO dto) {
        bannerService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除轮播图")
    public Result<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return Result.success();
    }
}
