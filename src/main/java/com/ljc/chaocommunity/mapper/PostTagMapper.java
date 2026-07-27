package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljc.chaocommunity.pojo.entity.PostTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostTagMapper extends BaseMapper<PostTag> {

    int insertBatch(@Param("list") List<PostTag> list);

}
