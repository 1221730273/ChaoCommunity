package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> listAll();

    Category getById(Long id);

    void create(Category category);

    void update(Category category);

    void delete(Long id);
}
