package com.ljc.chaocommunity.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理 + 消息推送
 * 同一用户支持多 tab 多连接；发送前判 isOpen，失败就地清理失效会话
 */
@Component
public class WebSocketService {

    /** userId → 会话集合 */
    private final Map<Long, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Autowired
    private ObjectMapper objectMapper;

    public void addSession(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = sessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessions.remove(userId);
            }
        }
    }

    /**
     * 给指定用户推送消息（不在线则忽略）
     * @param payload 任意对象，内部用项目 ObjectMapper 序列化（已支持 LocalDateTime）
     */
    public void sendToUser(Long userId, Object payload) {
        Set<WebSocketSession> set = sessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return;
        }
        for (WebSocketSession session : set) {
            if (!session.isOpen()) {
                removeSession(userId, session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                removeSession(userId, session);
            }
        }
    }
}
