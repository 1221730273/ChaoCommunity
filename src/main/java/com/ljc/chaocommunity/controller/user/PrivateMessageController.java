package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.dto.PrivateReadDTO;
import com.ljc.chaocommunity.pojo.dto.PrivateSendDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.ConversationVO;
import com.ljc.chaocommunity.pojo.vo.PrivateMessageVO;
import com.ljc.chaocommunity.service.PrivateMessageService;
import com.ljc.chaocommunity.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/private")
@Tag(name = "私信")
public class PrivateMessageController {

    @Autowired
    private PrivateMessageService privateMessageService;

    @PostMapping("/message/send")
    @Operation(summary = "发送私信")
    public Result<PrivateMessageVO> send(@Valid @RequestBody PrivateSendDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(privateMessageService.sendMessage(userId, dto.getTargetUserId(), dto.getContent()));
    }

    @GetMapping("/message/list")
    @Operation(summary = "会话聊天记录（page=1 为最新一页）")
    public Result<PageResult<PrivateMessageVO>> list(@RequestParam Long conversationId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(privateMessageService.pageMessages(userId, conversationId, page, size));
    }

    @PostMapping("/message/read")
    @Operation(summary = "打开会话标记已读（返回新全局未读数）")
    public Result<Integer> read(@Valid @RequestBody PrivateReadDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(privateMessageService.readConversation(userId, dto.getConversationId()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "私信未读数量")
    public Result<Integer> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(privateMessageService.unreadCount(userId));
    }

    @GetMapping("/conversation/list")
    @Operation(summary = "会话列表（分页，按最后消息时间倒序）")
    public Result<PageResult<ConversationVO>> conversations(@RequestParam(defaultValue = "1") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(privateMessageService.pageConversations(userId, page, size));
    }
}
