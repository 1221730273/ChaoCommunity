package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户端轮播图
 */
@Data
@Schema(description = "轮播图（用户端）")
public class BannerVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "图片地址")
    private String imgUrl;

    @Schema(description = "跳转链接")
    private String linkUrl;
}
