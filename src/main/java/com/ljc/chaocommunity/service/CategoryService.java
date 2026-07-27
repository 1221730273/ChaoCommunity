package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.entity.Category;

import java.util.List;

public interface CategoryService {

    /**
     * 查询所有分类（按 sort 排序）
     */
    List<Category> listAll();
}
