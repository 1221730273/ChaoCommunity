package com.ljc.chaocommunity.controller.user;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.pojo.vo.NotificationVO;
import com.ljc.chaocommunity.service.NotificationService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户消息通知接口（全部需登录，走 anyRequest().authenticated() 默认规则）
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /** 分页查询通知列表，tab: all/comment/like/follow/system */
    @GetMapping("/list")
    public Result<PageResult<NotificationVO>> list(@RequestParam(required = false) String tab,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.pageQuery(userId, tab, page, size));
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(notificationService.unreadCount(userId));
    }

    /** 标记单条已读 */
    @PutMapping("/read/{id}")
    public Result<Void> read(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markRead(userId, id);
        return Result.success();
    }

    /** 全部已读 */
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.success();
    }
}
