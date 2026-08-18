package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.ConversationVO;
import com.ljc.chaocommunity.pojo.vo.PrivateMessageVO;

/**
 * 私信服务
 */
public interface PrivateMessageService {

    /**
     * 发送私信（同步：保存 MySQL + Redis 未读 +1 + WebSocket 推送）。
     * 被核心规则拦截时抛 BusinessException。
     */
    PrivateMessageVO sendMessage(Long userId, Long targetUserId, String content);

    /** 分页查询会话聊天记录（page=1 为最新一页，返回升序便于直接渲染） */
    PageResult<PrivateMessageVO> pageMessages(Long userId, Long conversationId, int page, int size);

    /** 分页查询会话列表（按最后消息时间倒序） */
    PageResult<ConversationVO> pageConversations(Long userId, int page, int size);

    /** 打开会话标记已读（该会话对方发来的所有未读消息置已读），返回新的全局未读数 */
    int readConversation(Long userId, Long conversationId);

    /** 全局未读数 */
    int unreadCount(Long userId);
}
