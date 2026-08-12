package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户反馈请求")
public class FeedbackDTO {

    @NotBlank(message = "反馈类型不能为空")
    @Schema(description = "反馈类型 BUG/SUGGESTION/OTHER")
    private String type;

    @NotBlank(message = "反馈内容不能为空")
    @Schema(description = "反馈内容")
    private String content;

    @Schema(description = "联系方式（选填）")
    private String contact;
}
