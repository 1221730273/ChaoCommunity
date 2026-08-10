package com.ljc.chaocommunity.pojo.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Elasticsearch 用户文档 — 搜索 + 直接返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String signature;
    private Integer followCount;
    private Integer followerCount;
    private Integer role;
    /** 0=正常 1=封禁 */
    private Integer status;
    private LocalDateTime createTime;
}
