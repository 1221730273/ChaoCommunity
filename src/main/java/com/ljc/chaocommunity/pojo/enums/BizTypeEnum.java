package com.ljc.chaocommunity.pojo.enums;

/**
 * 文件业务类型枚举
 */
public enum BizTypeEnum {

    /** 用户头像 */
    AVATAR("用户头像"),

    /** 帖子封面 */
    POST_COVER("帖子封面"),

    /** 帖子内容中的图片 */
    POST_CONTENT("帖子内容");

    private final String description;

    BizTypeEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
