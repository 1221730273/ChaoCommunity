package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.TagDTO;
import com.ljc.chaocommunity.pojo.entity.Tag;

import java.util.List;

public interface TagService {

    List<Tag> listAll();

    Tag getById(Long id);

    void create(TagDTO dto);

    void update(TagDTO dto);

    void delete(Long id);
}
