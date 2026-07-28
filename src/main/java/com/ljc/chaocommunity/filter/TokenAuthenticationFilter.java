package com.ljc.chaocommunity.filter;

import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.pojo.entity.LoginInfo;
import com.ljc.chaocommunity.pojo.entity.LoginUser;
import com.ljc.chaocommunity.pojo.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Token 认证过滤器
 * 从请求头获取 token → Redis 查 LoginInfo → 转为 LoginUser → 设置 SecurityContext
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头获取 token
        String token = request.getHeader("token");
        if (!StringUtils.hasText(token)) {
            // 没 token 直接放行，交给后面的过滤器处理（登录接口不需要 token）
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 从 Redis 中获取用户信息
        LoginInfo loginInfo = (LoginInfo) redisTemplate.opsForValue().get("auth:token:" + token);
        if (Objects.isNull(loginInfo)) {
            throw new BusinessException("用户未登录");
        }

        // 3. LoginInfo → User → LoginUser
        User user = new User();
        BeanUtils.copyProperties(loginInfo, user);
        LoginUser loginUser = new LoginUser(user);

        // 4. 存入 SecurityContext，后续接口通过 SecurityContextHolder 获取当前用户
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 5. 放行
        filterChain.doFilter(request, response);
    }
}
