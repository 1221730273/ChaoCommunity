package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.BannerVO;
import com.ljc.chaocommunity.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/banner")
@Tag(name = "轮播图（用户端）")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    @Operation(summary = "查询展示中的轮播图")
    public Result<List<BannerVO>> list() {
        return Result.success(bannerService.getUserBanners());
    }
}
