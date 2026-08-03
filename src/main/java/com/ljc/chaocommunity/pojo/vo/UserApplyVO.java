package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端审核列表
 */
@Data
@Schema(description = "用户资料审核视图")
public class UserApplyVO {

    @Schema(description = "申请ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "审核类型")
    private String type;

    @Schema(description = "当前昵称")
    private String currentNickname;

    @Schema(description = "申请昵称")
    private String nickname;

    @Schema(description = "当前头像")
    private String currentAvatar;

    @Schema(description = "头像文件ID")
    private Long avatarFileId;

    @Schema(description = "新头像URL（从 file_record 查出）")
    private String avatarUrl;

    @Schema(description = "当前签名")
    private String currentSignature;

    @Schema(description = "申请签名")
    private String signature;

    @Schema(description = "审核状态")
    private Integer status;

    @Schema(description = "驳回原因")
    private String rejectReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
