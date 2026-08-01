package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "公告请求（创建/修改共用）")
public class AnnouncementDTO {

    @Schema(description = "公告ID（修改时必传）")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最多100字")
    @Schema(description = "公告标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "公告内容")
    private String content;

    @Schema(description = "公告类型 1系统 2活动 3更新")
    private Integer type;

    @Schema(description = "是否置顶 0否 1是")
    private Integer isTop;

    @Schema(description = "排序")
    private Integer sort;
}
