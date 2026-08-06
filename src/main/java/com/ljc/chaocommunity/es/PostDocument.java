package com.ljc.chaocommunity.es;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch 帖子文档 — 仅存搜索相关字段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDocument {

    private Long id;
    private String title;
    private String content;
    private Long categoryId;
    private String categoryName;
    private List<String> tags;
    private Long authorId;
    private String username;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer isTop;
    private Integer status;
}
