package com.ljc.chaocommunity.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论上下文（管理员查看某条评论时返回）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentContextVO {

    /** 目标类型：ROOT=一级评论, CHILD=二级回复 */
    private String targetType;

    /** 目标评论ID */
    private Long targetId;

    /** 父级评论（目标为一级评论时 = 目标评论自身，目标为二级评论时 = 其父评论） */
    private CommentVO rootComment;

    /** 该父评论下的所有子回复（按时间升序），目标评论 highlight=true */
    private List<CommentVO> children;
}
