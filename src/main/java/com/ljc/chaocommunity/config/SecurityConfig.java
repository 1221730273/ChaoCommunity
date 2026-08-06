package com.ljc.chaocommunity.config;

import com.ljc.chaocommunity.filter.TokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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

    /**
     * CORS 跨域配置
     * 前端开发服务器 localhost:5173 访问后端 localhost:8080 属于跨域，必须允许
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许本机任意端口的前端（开发模式 Vite 默认 5173）
        config.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        // 预检请求结果缓存 1 小时，避免频繁 OPTIONS
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 无状态，不创建 session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // 启用 CORS
                .cors(Customizer.withDefaults())
                // 权限规则
                .authorizeHttpRequests(auth -> auth
                        // ===== 完全放开的接口（游客可访问）=====

                        // 认证
                        .requestMatchers("/auth/login", "/auth/register").permitAll()

                        // CORS 预检请求全部放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 帖子浏览
                        .requestMatchers(HttpMethod.GET, "/post/list").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/featured").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/latest").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/featured/latest").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/user/**").permitAll()
                        // 帖子详情：仅匹配 /post/数字ID
                        .requestMatchers(new RegexRequestMatcher("/post/\\d+", "GET")).permitAll()
                        // 帖子搜索
                        .requestMatchers(HttpMethod.GET, "/post/search").permitAll()

                        // 用户主页
                        .requestMatchers(HttpMethod.GET, "/user/*/posts").permitAll()
                        .requestMatchers(HttpMethod.GET, "/user/*/profile").permitAll()
                        // 用户搜索
                        .requestMatchers(HttpMethod.GET, "/user/search").permitAll()

                        // /follow/me 需要认证（必须在 /* 通配之前）
                        .requestMatchers(HttpMethod.GET, "/follow/me/**").authenticated()

                        // 查别人的关注数据（公开）
                        .requestMatchers(HttpMethod.GET, "/follow/*/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/follow/*/following").permitAll()
                        .requestMatchers(HttpMethod.GET, "/follow/*/followers").permitAll()

                        // 评论浏览
                        .requestMatchers(HttpMethod.GET, "/comment/list").permitAll()
                        .requestMatchers(HttpMethod.GET, "/comment/context/**").permitAll()

                        // 分类
                        .requestMatchers(HttpMethod.GET, "/category/**").permitAll()

                        // 标签
                        .requestMatchers(HttpMethod.GET, "/tag/**").permitAll()

                        // 轮播图
                        .requestMatchers(HttpMethod.GET, "/banner/list").permitAll()

                        // 公告
                        .requestMatchers(HttpMethod.GET, "/announcement/**").permitAll()

                        // ===== 管理后台接口需要管理员角色 =====
                        .requestMatchers("/admin/**").hasRole("ADMIN")

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
