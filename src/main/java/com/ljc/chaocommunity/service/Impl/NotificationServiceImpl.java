package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.mapper.NotificationMapper;
import com.ljc.chaocommunity.pojo.entity.Notification;
import com.ljc.chaocommunity.pojo.enums.NotifyType;
import com.ljc.chaocommunity.pojo.mq.NotifyMessage;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.NotificationVO;
import com.ljc.chaocommunity.service.NotificationService;
import com.ljc.chaocommunity.websocket.WebSocketService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息通知服务实现
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    /** 未读数缓存 key 前缀 */
    private static final String UNREAD_KEY = "notify:unread:";
    /** 未读数缓存 TTL：7 天 */
    private static final long UNREAD_TTL = 7;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WebSocketService webSocketService;

    @Override
    public void save(NotifyMessage msg) {
        Notification n = new Notification();
        n.setReceiverId(msg.getReceiverId());
        n.setSenderId(msg.getSenderId());
        n.setSenderNickname(msg.getSenderNickname());
        n.setSenderAvatar(msg.getSenderAvatar());
        n.setType(msg.getType());
        n.setPostId(msg.getPostId());
        n.setPostTitle(msg.getPostTitle());
        n.setCommentId(msg.getCommentId());
        n.setParentCommentId(msg.getParentCommentId());
        n.setContent(msg.getContent());
        n.setParentContent(msg.getParentContent());
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    @Override
    public void pushAfterSave(NotifyMessage msg) {
        // 1. Redis 未读数 +1（返回新值作为权威未读数，顺带续期 TTL）
        String key = UNREAD_KEY + msg.getReceiverId();
        Long unread = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, UNREAD_TTL, TimeUnit.DAYS);

        // 2. WebSocket 推送：unreadCount + 通知 VO（补当前时间，实时插入列表可显示）
        NotificationVO vo = toVO(msg);
        vo.setCreateTime(LocalDateTime.now());
        webSocketService.sendToUser(msg.getReceiverId(), Map.of(
                "unreadCount", unread == null ? 1 : unread.intValue(),
                "notification", vo
        ));
    }

    @Override
    public PageResult<NotificationVO> pageQuery(Long userId, String tab, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId);
        List<Integer> types = NotifyType.codesOf(tab);
        if (types != null) {
            wrapper.in(Notification::getType, types);
        }
        wrapper.orderByDesc(Notification::getId);

        Page<Notification> p = notificationMapper.selectPage(new Page<>(page, size), wrapper);
        List<NotificationVO> voList = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), voList);
    }

    @Override
    public int unreadCount(Long userId) {
        String key = UNREAD_KEY + userId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        // Redis 无值（重启/TTL 过期）：用 DB COUNT 兜底初始化，setIfAbsent 避免并发覆盖增量
        Long dbCount = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0));
        redisTemplate.opsForValue().setIfAbsent(key, dbCount, UNREAD_TTL, TimeUnit.DAYS);
        return dbCount == null ? 0 : dbCount.intValue();
    }

    @Override
    public void markRead(Long userId, Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null || !n.getReceiverId().equals(userId)) {
            return; // 不存在或非本人，忽略
        }
        if (n.getIsRead() != null && n.getIsRead() == 1) {
            return; // 已读，防重复扣未读数
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .set(Notification::getIsRead, 1));
        // Redis 未读数 >0 才扣，避免 key 缺失/已归零时变成负数
        Object val = redisTemplate.opsForValue().get(UNREAD_KEY + userId);
        if (val instanceof Number && ((Number) val).intValue() > 0) {
            redisTemplate.opsForValue().decrement(UNREAD_KEY + userId);
        }
    }

    @Override
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        redisTemplate.opsForValue().set(UNREAD_KEY + userId, 0, UNREAD_TTL, TimeUnit.DAYS);
    }

    // ==================== VO 组装（快照直接搬，零查库） ====================

    private NotificationVO toVO(Notification n) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(n, vo);
        return vo;
    }

    private NotificationVO toVO(NotifyMessage msg) {
        NotificationVO vo = new NotificationVO();
        vo.setReceiverId(msg.getReceiverId());
        vo.setSenderId(msg.getSenderId());
        vo.setSenderNickname(msg.getSenderNickname());
        vo.setSenderAvatar(msg.getSenderAvatar());
        vo.setType(msg.getType());
        vo.setPostId(msg.getPostId());
        vo.setPostTitle(msg.getPostTitle());
        vo.setCommentId(msg.getCommentId());
        vo.setParentCommentId(msg.getParentCommentId());
        vo.setContent(msg.getContent());
        vo.setParentContent(msg.getParentContent());
        vo.setIsRead(0);
        return vo;
    }
}
