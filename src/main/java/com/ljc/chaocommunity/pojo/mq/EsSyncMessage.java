package com.ljc.chaocommunity.pojo.mq;

import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ES 同步消息体
 * 每种类型只使用部分字段，消费端按 type 分派：
 * - POST_INDEX / USER_INDEX：携带 post / user 实体
 * - POST_DELETE / 局部更新：携带 id
 * - POST_UPDATE_STATUS / POST_UPDATE_TOP：携带 id + value
 * - POST_UPDATE_*_COUNT：携带 id + delta
 * - POST_BATCH_HIDE_BY_USER：携带 userId
 */
@Data
@NoArgsConstructor
public class EsSyncMessage {

    /** 操作类型 */
    private EsSyncType type;

    /** 目标 ID（帖子/用户），用于删除/局部更新 */
    private Long id;

    /** 用户 ID，用于批量操作（如批量隐藏某用户帖子） */
    private Long userId;

    /** 数值（状态/置顶 0/1） */
    private Integer value;

    /** 增量（点赞/浏览/评论计数，正负皆可） */
    private Integer delta;

    /** 帖子实体（POST_INDEX 时携带） */
    private Post post;

    /** 用户实体（USER_INDEX 时携带） */
    private User user;
}
