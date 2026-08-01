package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告视图（管理端，含状态字段）
 */
@Data
@Schema(description = "公告管理视图")
public class AdminAnnouncementVO {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "公告类型")
    private Integer type;

    @Schema(description = "状态 0展示 1下架")
    private Integer status;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "发布管理员ID")
    private Long createUserId;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
