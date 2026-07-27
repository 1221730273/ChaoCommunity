package com.ljc.chaocommunity.controller;

import com.ljc.chaocommunity.pojo.entity.Category;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
@Tag(name = "分类管理")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询所有分类
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有分类")
    public Result<List<Category>> listCategories() {
        List<Category> categories = categoryService.listAll();
        return Result.success(categories);
    }
}
