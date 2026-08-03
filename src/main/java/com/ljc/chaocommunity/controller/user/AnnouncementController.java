package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.AnnouncementVO;
import com.ljc.chaocommunity.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/announcement")
@Tag(name = "公告管理")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping("/list")
    @Operation(summary = "分页查询公告列表")
    public Result<PageResult<AnnouncementVO>> list(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.success(announcementService.list(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "公告详情")
    public Result<AnnouncementVO> getDetail(@PathVariable Long id) {
        return Result.success(announcementService.getDetail(id));
    }

    @GetMapping("/latest")
    @Operation(summary = "最新公告")
    public Result<List<AnnouncementVO>> getLatest(@RequestParam(defaultValue = "4") int limit) {
        return Result.success(announcementService.getLatest(limit));
    }
}
