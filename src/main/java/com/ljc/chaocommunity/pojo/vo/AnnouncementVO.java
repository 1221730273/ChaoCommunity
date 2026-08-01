package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告视图（用户端）
 */
@Data
@Schema(description = "公告视图")
public class AnnouncementVO {

    @Schema(description = "公告ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "公告类型")
    private Integer type;

    @Schema(description = "是否置顶")
    private Integer isTop;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
