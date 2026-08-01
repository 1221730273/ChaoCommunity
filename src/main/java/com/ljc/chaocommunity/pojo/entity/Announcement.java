package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告实体
 */
@Data
@TableName("announcement")
public class Announcement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 公告类型 1系统公告 2活动公告 3更新公告 */
    private Integer type;

    /** 状态 0展示 1下架 */
    private Integer status;

    /** 是否置顶 0否 1是 */
    private Integer isTop;

    /** 排序 */
    private Integer sort;

    /** 浏览量 */
    private Integer viewCount;

    /** 发布管理员ID */
    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
