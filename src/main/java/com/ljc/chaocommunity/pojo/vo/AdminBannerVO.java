package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理端轮播图（含管理字段）
 */
@Data
@Schema(description = "轮播图（管理端）")
public class AdminBannerVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片地址")
    private String imgUrl;

    @Schema(description = "文件记录ID")
    private Long fileId;

    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态 0关闭 1展示")
    private Integer status;
}
