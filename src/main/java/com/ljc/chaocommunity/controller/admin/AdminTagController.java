package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.dto.TagDTO;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tag")
@Tag(name = "标签管理（管理端）")
public class AdminTagController {

    @Autowired
    private TagService tagService;

    @PostMapping
    @Operation(summary = "新增标签")
    public Result<Void> create(@Valid @RequestBody TagDTO dto) {
        tagService.create(dto);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "修改标签")
    public Result<Void> update(@Valid @RequestBody TagDTO dto) {
        tagService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
}
