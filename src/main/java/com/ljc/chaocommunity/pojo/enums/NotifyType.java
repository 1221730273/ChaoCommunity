package com.ljc.chaocommunity.pojo.enums;

import java.util.List;

/**
 * 通知类型枚举
 * 1回复帖子 2回复评论 3点赞帖子 4点赞评论 5关注 6系统
 */
public enum NotifyType {

    REPLY_POST(1),
    REPLY_COMMENT(2),
    LIKE_POST(3),
    LIKE_COMMENT(4),
    FOLLOW(5),
    SYSTEM(6);

    private final int code;

    NotifyType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    /**
     * 前端 tab → 类型集合
     * @param tab all/comment/like/follow/system
     * @return 类型集合；all 或未知值返回 null（表示不过滤）
     */
    public static List<Integer> codesOf(String tab) {
        if (tab == null) {
            return null;
        }
        return switch (tab) {
            case "comment" -> List.of(REPLY_POST.code, REPLY_COMMENT.code);
            case "like" -> List.of(LIKE_POST.code, LIKE_COMMENT.code);
            case "follow" -> List.of(FOLLOW.code);
            case "system" -> List.of(SYSTEM.code);
            default -> null; // all / 未知值
        };
    }
}
