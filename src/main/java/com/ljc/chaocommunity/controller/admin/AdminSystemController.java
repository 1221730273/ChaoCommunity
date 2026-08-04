package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system")
@Tag(name = "系统管理（管理端）")
public class AdminSystemController {

    @Autowired
    private FileService fileService;

    @PostMapping("/clean-temp-files")
    @Operation(summary = "手动触发临时文件清理")
    public Result<Void> cleanTempFiles() {
        fileService.cleanTempFile();
        return Result.success();
    }
}
