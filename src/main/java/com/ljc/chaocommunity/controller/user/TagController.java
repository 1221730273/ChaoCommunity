package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.entity.Tag;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tag")
@io.swagger.v3.oas.annotations.tags.Tag(name = "标签管理")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping("/list")
    @Operation(summary = "查询所有标签")
    public Result<List<Tag>> listAll() {
        return Result.success(tagService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询单个标签")
    public Result<Tag> getById(@PathVariable Long id) {
        return Result.success(tagService.getById(id));
    }
}
