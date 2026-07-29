package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改用户资料")
public class UserProfileDTO {

    @Size(max = 32, message = "昵称最多32个字")
    @Schema(description = "昵称")
    private String nickname;

    @Size(max = 64, message = "邮箱最多64个字符")
    @Schema(description = "邮箱")
    private String email;

    @Size(max = 256, message = "个性签名最多256个字")
    @Schema(description = "个性签名")
    private String signature;
}
