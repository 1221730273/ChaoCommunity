package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子详情视图")
public class PostVO implements Serializable {

    // ========== 帖子信息 ==========
    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "发帖人ID")
    private Long userId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "帖子标题")
    private String title;

    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "封面图片URL")
    private String coverUrl;

    @Schema(description = "是否置顶 0否 1是")
    private Integer top;

    @Schema(description = "状态 0正常 1封禁")
    private Integer status;

    @Schema(description = "是否精选 0否 1是")
    private Integer isFeatured;

    @Schema(description = "发帖时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ========== 发帖人信息 ==========
    @Schema(description = "发帖人用户名")
    private String username;

    @Schema(description = "发帖人昵称")
    private String nickname;

    @Schema(description = "发帖人头像")
    private String avatar;

    // ========== 标签列表 ==========
    @Schema(description = "帖子标签")
    private List<TagVO> tags;

}
