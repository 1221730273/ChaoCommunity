package com.ljc.chaocommunity.controller.admin;

import com.ljc.chaocommunity.pojo.result.Result;
import com.ljc.chaocommunity.service.FileService;
import com.ljc.chaocommunity.service.Impl.AnnouncementServiceImpl;
import com.ljc.chaocommunity.service.Impl.BannerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/admin/system")
@Tag(name = "系统管理（管理端）")
public class AdminSystemController {

    @Autowired
    private FileService fileService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/clean-temp-files")
    @Operation(summary = "手动触发临时文件清理")
    public Result<Void> cleanTempFiles() {
        fileService.cleanTempFile();
        return Result.success();
    }

    @PostMapping("/clear-banner-cache")
    @Operation(summary = "手动清空轮播图缓存")
    public Result<Void> clearBannerCache() {
        redisTemplate.delete(BannerServiceImpl.HOME_BANNERS_CACHE_KEY);
        return Result.success();
    }

    @PostMapping("/clear-announcement-cache")
    @Operation(summary = "手动清空最新公告缓存")
    public Result<Void> clearAnnouncementCache() {
        Set<String> keys = redisTemplate.keys(AnnouncementServiceImpl.HOME_ANNOUNCEMENTS_CACHE_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        return Result.success();
    }
}
