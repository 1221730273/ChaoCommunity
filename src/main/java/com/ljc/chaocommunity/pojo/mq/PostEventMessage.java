package com.ljc.chaocommunity.pojo.mq;

import com.ljc.chaocommunity.pojo.enums.PostEventType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子事件消息（异步失效精选缓存）
 * 携带 isFeatured（操作时该帖子是否为精选），消费者据此决定是否需要清理精选缓存
 */
@Data
@NoArgsConstructor
public class PostEventMessage {

    /** 事件类型 */
    private PostEventType type;

    /** 帖子ID */
    private Long postId;

    /** 操作时该帖子是否为精选（是否影响精选缓存） */
    private Boolean isFeatured;
}
