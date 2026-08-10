package com.ljc.chaocommunity.pojo.redis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户缓存（Redis），剔除关注数、粉丝数，二者实时维护")
public class UserCache implements Serializable {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "个性签名")
    private String signature;

    @Schema(description = "角色 0=普通用户 1=管理员")
    private Integer role;

    @Schema(description = "状态 0=正常 1=封禁")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

}
