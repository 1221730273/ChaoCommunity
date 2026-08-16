package com.ljc.chaocommunity.pojo.enums;

/**
 * 帖子事件类型（用于异步失效精选缓存等）
 */
public enum PostEventType {

    /** 帖子被删除（用户/管理员） */
    POST_DELETED,
    /** 帖子审核通过（内容/封面更新，或新帖） */
    AUDIT_APPROVED,
    /** 精选状态切换（设置精选/取消精选） */
    FEATURED_TOGGLED
}
