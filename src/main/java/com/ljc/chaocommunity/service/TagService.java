package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.entity.Tag;

import java.util.List;

public interface TagService {

    List<Tag> listAll();

    Tag getById(Long id);

    void create(Tag tag);

    void update(Tag tag);

    void delete(Long id);
}
