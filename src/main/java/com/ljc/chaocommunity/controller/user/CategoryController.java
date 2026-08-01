package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.entity.Category;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@Tag(name = "分类管理")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "查询所有分类")
    public Result<List<Category>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询单个分类")
    public Result<Category> getById(@PathVariable Long id) {
        return Result.success(categoryService.getById(id));
    }
}
