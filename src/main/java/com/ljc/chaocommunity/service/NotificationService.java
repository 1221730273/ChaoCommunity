package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.mq.NotifyMessage;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.NotificationVO;

/**
 * 消息通知服务
 */
public interface NotificationService {

    /** 通知入库（NotifyConsumer 第一步，仅 insert） */
    void save(NotifyMessage msg);

    /** 未读数 +1 并 WebSocket 推送（NotifyConsumer 第二步，非事务） */
    void pushAfterSave(NotifyMessage msg);

    /** 分页查询通知列表（tab: all/comment/like/follow/system） */
    PageResult<NotificationVO> pageQuery(Long userId, String tab, int page, int size);

    /** 未读数（Redis 优先，空则 DB 兜底初始化） */
    int unreadCount(Long userId);

    /** 标记单条已读（校验归属 + 防重复扣） */
    void markRead(Long userId, Long id);

    /** 全部已读 */
    void markAllRead(Long userId);
}
