package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体
 */
@Data
@TableName("post")
public class Post {


    /**
     * 帖子ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;


    /**
     * 发帖用户ID
     */
    private Long userId;


    /**
     * 所属分类ID
     */
    private Long categoryId;


    /**
     * 帖子标题
     */
    private String title;


    /**
     * 帖子正文
     */
    private String content;


    /**
     * 浏览量
     */
    private Integer viewCount;


    /**
     * 点赞数量
     */
    private Integer likeCount;

    /**
     * 评论数量（冗余字段）
     */
    private Integer commentCount;

    /**
     * 封面图片URL
     */
    private String coverUrl;


    /**
     * 是否置顶
     * 0 否
     * 1 是
     */
    private Integer top;


    /**
     * 帖子状态
     * 0 展示
     * 1 不展示
     */
    private Integer status;


    /**
     * 是否精选
     * 0 否
     * 1 是
     */
    private Integer isFeatured;


    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    /**
     * 逻辑删除
     * 0 未删除
     * 1 已删除
     */
    @TableLogic
    private Integer deleted;

}
