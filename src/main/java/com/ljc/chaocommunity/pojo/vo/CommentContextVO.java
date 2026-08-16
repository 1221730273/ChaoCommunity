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

    /** 根评论（目标为一级评论时 = 目标评论自身，否则 = 祖先链最顶端的根评论） */
    private CommentVO rootComment;

    /** 目标评论的完整祖先链（根 → ... → 目标，含目标自身），按层级从浅到深 */
    private List<CommentVO> chain;

    /** 目标评论的直接子回复（按时间升序） */
    private List<CommentVO> children;
}
