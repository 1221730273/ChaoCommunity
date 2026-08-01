package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户资料视图
 */
@Data
@Schema(description = "用户资料视图")
public class UserVO {

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

    @Schema(description = "邮箱（仅自己可见）")
    private String email;

    @Schema(description = "关注数")
    private Integer followCount;

    @Schema(description = "粉丝数")
    private Integer followerCount;

    @Schema(description = "角色 0=普通用户 1=管理员")
    private Integer role;

    @Schema(description = "状态 0=正常 1=禁言")
    private Integer status;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;
}
