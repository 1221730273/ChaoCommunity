package com.ljc.chaocommunity.util;

import com.ljc.chaocommunity.pojo.entity.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Security 工具类，获取当前登录用户信息
 */
public class SecurityUtils {

    /**
     * 获取当前登录用户的 LoginUser
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        return (LoginUser) authentication.getPrincipal();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        return getLoginUser().getUser().getId();
    }

    /**
     * 获取当前登录用户名
     */
    public static String getCurrentUsername() {
        return getLoginUser().getUser().getUsername();
    }
}
