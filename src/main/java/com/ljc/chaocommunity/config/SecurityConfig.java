package com.ljc.chaocommunity.config;

import com.ljc.chaocommunity.filter.TokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

/**
 * Spring Security 安全配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 无状态，不创建 session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 权限规则
                .authorizeHttpRequests(auth -> auth
                        // ===== 完全放开的接口（游客可访问）=====

                        // 认证
                        .requestMatchers("/auth/login", "/auth/register").permitAll()

                        // 帖子浏览
                        .requestMatchers(HttpMethod.GET, "/post/list").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/featured").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/user/**").permitAll()
                        // 帖子详情：仅匹配 /post/数字ID
                        .requestMatchers(new RegexRequestMatcher("/post/\\d+", "GET")).permitAll()

                        // 用户主页
                        .requestMatchers(HttpMethod.GET, "/user/*/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user/*/profile").permitAll()

                        // /follow/me 需要认证（必须在 /* 通配之前）
                        .requestMatchers(HttpMethod.GET, "/follow/me/**").authenticated()

                        // 查别人的关注数据（公开）
                        .requestMatchers(HttpMethod.GET, "/follow/*/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/follow/*/following").permitAll()
                        .requestMatchers(HttpMethod.GET, "/follow/*/followers").permitAll()

                        // 分类
                        .requestMatchers(HttpMethod.GET, "/category/**").permitAll()

                        // 标签
                        .requestMatchers(HttpMethod.GET, "/tag/**").permitAll()

                        // 轮播图
                        .requestMatchers(HttpMethod.GET, "/banner/list").permitAll()

                        // 公告
                        .requestMatchers(HttpMethod.GET, "/announcement/**").permitAll()

                        // ===== 其余接口需要认证 =====
                        .anyRequest().authenticated()
                )
                // 关闭 CSRF
                .csrf(csrf -> csrf.disable())
                // Token 认证过滤器
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
