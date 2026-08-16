package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljc.chaocommunity.pojo.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户消息通知 Mapper（单表查询，使用 MyBatis-Plus 通用方法）
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
