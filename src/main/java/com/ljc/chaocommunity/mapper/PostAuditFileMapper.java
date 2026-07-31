package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljc.chaocommunity.pojo.entity.PostAuditFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostAuditFileMapper extends BaseMapper<PostAuditFile> {

    int insertBatch(@Param("list") List<PostAuditFile> list);
}
