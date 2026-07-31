package com.ljc.chaocommunity.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录用） */
    private String username;

    /** BCrypt加密后的密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 头像文件记录ID */
    private Long avatarFileId;

    /** 邮箱 */
    private String email;

    /** 个性签名 */
    private String signature;

    /** 关注数 */
    private Integer followCount;

    /** 被关注数（粉丝数） */
    private Integer followerCount;

    /** 角色 0=普通用户 1=管理员 */
    private Integer role;

    /** 用户状态 0=正常 1=禁言 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
