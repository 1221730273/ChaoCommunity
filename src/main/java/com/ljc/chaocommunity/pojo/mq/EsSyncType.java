package com.ljc.chaocommunity.pojo.mq;

/**
 * ES 同步消息类型
 * 定义所有通过消息队列异步执行的 ES 写操作（增删改帖子/用户后触发）
 */
public enum EsSyncType {

    /** 写入/覆盖一篇帖子（发帖审核通过、内容/封面变更后） */
    POST_INDEX,
    /** 删除一篇帖子 */
    POST_DELETE,
    /** 更新帖子状态（隐藏/公开） */
    POST_UPDATE_STATUS,
    /** 更新帖子置顶状态 */
    POST_UPDATE_TOP,
    /** 更新帖子点赞数（delta 增量） */
    POST_UPDATE_LIKE_COUNT,
    /** 更新帖子浏览数（delta 增量） */
    POST_UPDATE_VIEW_COUNT,
    /** 更新帖子评论数（delta 增量） */
    POST_UPDATE_COMMENT_COUNT,
    /** 批量隐藏某用户的全部帖子（封禁用户时） */
    POST_BATCH_HIDE_BY_USER,
    /** 写入/覆盖一个用户（注册、资料变更后） */
    USER_INDEX,
    /** 更新用户封禁状态 */
    USER_UPDATE_STATUS
}
