package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发送私信请求")
public class PrivateSendDTO {

    @NotNull(message = "目标用户ID不能为空")
    @Schema(description = "目标用户ID")
    private Long targetUserId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 500, message = "消息最多500字")
    @Schema(description = "消息内容")
    private String content;
}
