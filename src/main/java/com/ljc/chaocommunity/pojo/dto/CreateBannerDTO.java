package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "新增轮播图请求")
public class CreateBannerDTO {

    @NotNull(message = "文件ID不能为空")
    @Schema(description = "图片文件ID")
    private Long fileId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "跳转链接")
    private String linkUrl;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态 0关闭 1展示")
    private Integer status;
}
