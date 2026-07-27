package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljc.chaocommunity.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
