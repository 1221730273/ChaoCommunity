package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子审核视图（管理端用）
 */
@Data
@Schema(description = "帖子审核视图")
public class PostAuditVO {

    @Schema(description = "审核记录ID")
    private Long id;

    @Schema(description = "提交用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "原帖子ID（null=新帖）")
    private Long postId;

    @Schema(description = "帖子标题")
    private String title;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "封面文件ID")
    private Long coverFileId;

    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "标签ID逗号分隔")
    private String tagIds;

    @Schema(description = "正文图片文件ID列表")
    private List<Long> contentFileIds;

    @Schema(description = "状态 0待审核 1通过 2拒绝")
    private Integer status;

    @Schema(description = "拒绝原因")
    private String rejectReason;

    @Schema(description = "处理人ID")
    private Long handlerId;

    @Schema(description = "提交时间")
    private LocalDateTime createTime;
}
