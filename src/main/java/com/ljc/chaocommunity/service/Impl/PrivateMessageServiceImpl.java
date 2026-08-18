package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.PrivateConversationMapper;
import com.ljc.chaocommunity.mapper.PrivateMessageMapper;
import com.ljc.chaocommunity.mapper.UserFollowMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.entity.PrivateConversation;
import com.ljc.chaocommunity.pojo.entity.PrivateMessage;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.entity.UserFollow;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.ConversationVO;
import com.ljc.chaocommunity.pojo.vo.PrivateMessageVO;
import com.ljc.chaocommunity.service.PrivateMessageService;
import com.ljc.chaocommunity.websocket.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 私信服务实现
 * <p>
 * 核心规则：会话 status=0 表示"等待对方回应"。此时只有接收者(user2)能回复
 * （回复即解锁 status=1）；发起者(user1)需等对方回应、或对方已关注自己，才能继续发。
 * <p>
 * 发送走同步接口（发送者需立即拿到结果），Redis 未读 + WebSocket 推送各自 try/catch，
 * 失败不回滚已入库的消息。
 */
@Slf4j
@Service
public class PrivateMessageServiceImpl implements PrivateMessageService {

    /** 未读数缓存 key 前缀 */
    private static final String UNREAD_KEY = "private:unread:";
    /** 未读数缓存 TTL：7 天 */
    private static final long UNREAD_TTL = 7;

    @Autowired
    private PrivateConversationMapper privateConversationMapper;

    @Autowired
    private PrivateMessageMapper privateMessageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WebSocketService webSocketService;

    @Override
    @Transactional
    public PrivateMessageVO sendMessage(Long userId, Long targetUserId, String content) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能给自己发私信");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException("用户不存在");
        }

        // 1. 查找或创建会话（user1 = 发起者，user2 = 接收者）
        LambdaQueryWrapper<PrivateConversation> cw = new LambdaQueryWrapper<>();
        cw.and(w -> w.eq(PrivateConversation::getUser1Id, userId).eq(PrivateConversation::getUser2Id, targetUserId)
                .or().eq(PrivateConversation::getUser1Id, targetUserId).eq(PrivateConversation::getUser2Id, userId));
        PrivateConversation conversation = privateConversationMapper.selectOne(cw);

        boolean statusChanged = false;
        if (conversation == null) {
            conversation = new PrivateConversation();
            conversation.setUser1Id(userId); // 发起者
            conversation.setUser2Id(targetUserId);
            conversation.setStatus(0); // 等待对方回应
            privateConversationMapper.insert(conversation);
        } else if (conversation.getStatus() != null && conversation.getStatus() == 0) {
            // 核心规则：status=0 时，只有接收者(user2)能回复；发起者(user1)须等对方回应或被对方关注
            Long user1 = conversation.getUser1Id();
            Long user2 = conversation.getUser2Id();
            if (userId.equals(user1)) {
                // 发起者：仅当对方(user2)已关注我(user1)时解锁（B 关注 A 可解除限制）
                boolean followed = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, user2)
                        .eq(UserFollow::getFolloweeId, user1)) > 0;
                if (!followed) {
                    throw new BusinessException("对方还未回复，请先等对方回应");
                }
            }
            // userId.equals(user2)：接收者回复，放行并解锁
            conversation.setStatus(1);
            statusChanged = true;
        }

        // 2. 插入消息（is_read=0：对方未读。自己发的消息由查询里的 sender_id != 我 天然排除，无需特判）
        PrivateMessage msg = new PrivateMessage();
        msg.setConversationId(conversation.getId());
        msg.setSenderId(userId);
        msg.setContent(content);
        msg.setIsRead(0);
        privateMessageMapper.insert(msg);

        // 3. 更新会话 last_message_id（可能同时带 status）；updateById 触发 updateTime 自动填充
        PrivateConversation update = new PrivateConversation();
        update.setId(conversation.getId());
        update.setLastMessageId(msg.getId());
        if (statusChanged) {
            update.setStatus(1);
        }
        privateConversationMapper.updateById(update);

        // 4. 组装 VO（发送者信息：发送方气泡和 WS 推送都用它）
        User sender = userMapper.selectById(userId);
        PrivateMessageVO vo = toVO(msg, sender);

        // 5. Redis 未读数 +1（失败仅角标短暂不准，DB 兜底自愈，不影响消息入库）
        Integer unread = null;
        try {
            String key = UNREAD_KEY + targetUserId;
            Long inc = redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, UNREAD_TTL, TimeUnit.DAYS);
            unread = inc == null ? 1 : inc.intValue();
        } catch (Exception e) {
            log.warn("私信未读数更新失败: {}", e.getMessage());
        }

        // 6. WebSocket 推送（接收者不在线则忽略）
        try {
            webSocketService.sendToUser(targetUserId, Map.of(
                    "type", "PRIVATE_MESSAGE",
                    "data", Map.of(
                            "conversationId", conversation.getId(),
                            "message", vo,
                            "unreadCount", unread == null ? unreadCount(targetUserId) : unread
                    )
            ));
        } catch (Exception e) {
            log.warn("私信 WebSocket 推送失败: {}", e.getMessage());
        }

        return vo;
    }

    @Override
    public PageResult<PrivateMessageVO> pageMessages(Long userId, Long conversationId, int page, int size) {
        PrivateConversation conversation = privateConversationMapper.selectById(conversationId);
        if (conversation == null
                || !(userId.equals(conversation.getUser1Id()) || userId.equals(conversation.getUser2Id()))) {
            throw new BusinessException("无权查看该会话");
        }

        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrivateMessage::getConversationId, conversationId)
                .orderByDesc(PrivateMessage::getId);
        Page<PrivateMessage> p = privateMessageMapper.selectPage(new Page<>(page, size), wrapper);
        // 最新一页是倒序，反转成升序便于前端直接渲染（page=1 为最新一页）
        List<PrivateMessage> records = p.getRecords();
        Collections.reverse(records);
        return new PageResult<>(p.getTotal(), toVOList(records));
    }

    @Override
    public PageResult<ConversationVO> pageConversations(Long userId, int page, int size) {
        LambdaQueryWrapper<PrivateConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(PrivateConversation::getUser1Id, userId).or().eq(PrivateConversation::getUser2Id, userId))
                .orderByDesc(PrivateConversation::getUpdateTime);
        Page<PrivateConversation> p = privateConversationMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getTotal(), buildConversationVOList(userId, p.getRecords()));
    }

    private List<ConversationVO> buildConversationVOList(Long userId, List<PrivateConversation> conversations) {
        if (conversations.isEmpty()) {
            return new ArrayList<>();
        }

        // 对方用户信息（批量）
        Set<Long> otherIds = new HashSet<>();
        for (PrivateConversation c : conversations) {
            otherIds.add(c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id());
        }
        Map<Long, User> userMap = new HashMap<>();
        if (!otherIds.isEmpty()) {
            userMapper.selectBatchIds(otherIds).forEach(u -> userMap.put(u.getId(), u));
        }

        // 最后一条消息（批量）
        Set<Long> lastMsgIds = conversations.stream()
                .map(PrivateConversation::getLastMessageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, PrivateMessage> lastMsgMap = new HashMap<>();
        if (!lastMsgIds.isEmpty()) {
            privateMessageMapper.selectBatchIds(lastMsgIds).forEach(m -> lastMsgMap.put(m.getId(), m));
        }

        // 每会话未读数：对方发来的未读消息（is_read=0），按会话分组统计
        List<Long> convIds = conversations.stream().map(PrivateConversation::getId).collect(Collectors.toList());
        Map<Long, Long> unreadMap = new HashMap<>();
        if (!convIds.isEmpty()) {
            privateMessageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                            .in(PrivateMessage::getConversationId, convIds)
                            .ne(PrivateMessage::getSenderId, userId)
                            .eq(PrivateMessage::getIsRead, 0)
                            .select(PrivateMessage::getConversationId))
                    .forEach(m -> unreadMap.merge(m.getConversationId(), 1L, Long::sum));
        }

        List<ConversationVO> result = new ArrayList<>();
        for (PrivateConversation c : conversations) {
            ConversationVO vo = new ConversationVO();
            vo.setId(c.getId());
            Long other = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            vo.setOtherUserId(other);
            User u = userMap.get(other);
            if (u != null) {
                vo.setOtherNickname(u.getNickname());
                vo.setOtherAvatar(u.getAvatar());
            }
            vo.setStatus(c.getStatus());
            vo.setWaiting(c.getStatus() != null && c.getStatus() == 0 && userId.equals(c.getUser1Id()));
            vo.setLastMessageId(c.getLastMessageId());
            PrivateMessage last = lastMsgMap.get(c.getLastMessageId());
            if (last != null) {
                vo.setLastMessage(last.getContent());
                vo.setLastSenderId(last.getSenderId());
                vo.setLastMessageTime(last.getCreateTime());
            }
            vo.setUnreadCount(unreadMap.getOrDefault(c.getId(), 0L).intValue());
            result.add(vo);
        }
        return result;
    }

    @Override
    public int readConversation(Long userId, Long conversationId) {
        PrivateConversation conversation = privateConversationMapper.selectById(conversationId);
        if (conversation == null
                || !(userId.equals(conversation.getUser1Id()) || userId.equals(conversation.getUser2Id()))) {
            throw new BusinessException("无权查看该会话");
        }

        // 当前会话对方发来的所有未读消息，一次性全部置已读
        Long unread = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                .eq(PrivateMessage::getConversationId, conversationId)
                .ne(PrivateMessage::getSenderId, userId)
                .eq(PrivateMessage::getIsRead, 0));
        if (unread != null && unread > 0) {
            privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getConversationId, conversationId)
                    .ne(PrivateMessage::getSenderId, userId)
                    .eq(PrivateMessage::getIsRead, 0)
                    .set(PrivateMessage::getIsRead, 1));
            // Redis 未读数 >0 才扣（防负数），只扣这一个会话的未读
            Object val = redisTemplate.opsForValue().get(UNREAD_KEY + userId);
            if (val instanceof Number && ((Number) val).intValue() > 0) {
                long toSub = Math.min(((Number) val).intValue(), unread);
                redisTemplate.opsForValue().decrement(UNREAD_KEY + userId, toSub);
            }
        }
        return unreadCount(userId);
    }

    @Override
    public int unreadCount(Long userId) {
        String key = UNREAD_KEY + userId;
        Object val = redisTemplate.opsForValue().get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        // Redis 无值（重启/TTL 过期）：DB COUNT 兜底初始化，只统计自己参与的会话中的未读
        List<Long> convIds = myConversationIds(userId);
        long dbCount = 0L;
        if (!convIds.isEmpty()) {
            Long c = privateMessageMapper.selectCount(new LambdaQueryWrapper<PrivateMessage>()
                    .in(PrivateMessage::getConversationId, convIds)
                    .ne(PrivateMessage::getSenderId, userId)
                    .eq(PrivateMessage::getIsRead, 0));
            dbCount = c == null ? 0 : c;
        }
        redisTemplate.opsForValue().setIfAbsent(key, dbCount, UNREAD_TTL, TimeUnit.DAYS);
        return (int) dbCount;
    }

    /** 当前用户参与的会话 ID 列表 */
    private List<Long> myConversationIds(Long userId) {
        LambdaQueryWrapper<PrivateConversation> cw = new LambdaQueryWrapper<>();
        cw.and(w -> w.eq(PrivateConversation::getUser1Id, userId).or().eq(PrivateConversation::getUser2Id, userId))
                .select(PrivateConversation::getId);
        return privateConversationMapper.selectList(cw).stream()
                .map(PrivateConversation::getId)
                .collect(Collectors.toList());
    }

    // ==================== VO 组装 ====================

    private List<PrivateMessageVO> toVOList(List<PrivateMessage> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> senderIds = records.stream().map(PrivateMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            userMapper.selectBatchIds(senderIds).forEach(u -> userMap.put(u.getId(), u));
        }
        List<PrivateMessageVO> list = new ArrayList<>();
        for (PrivateMessage m : records) {
            list.add(toVO(m, userMap.get(m.getSenderId())));
        }
        return list;
    }

    private PrivateMessageVO toVO(PrivateMessage m, User sender) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(m.getId());
        vo.setConversationId(m.getConversationId());
        vo.setSenderId(m.getSenderId());
        vo.setContent(m.getContent());
        vo.setIsRead(m.getIsRead());
        vo.setCreateTime(m.getCreateTime());
        if (sender != null) {
            vo.setSenderNickname(sender.getNickname());
            vo.setSenderAvatar(sender.getAvatar());
        }
        return vo;
    }
}
