package com.ljc.chaocommunity.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ljc.chaocommunity.pojo.es.PostDocument;
import com.ljc.chaocommunity.mapper.*;
import com.ljc.chaocommunity.pojo.entity.*;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.pojo.vo.TagVO;
import com.ljc.chaocommunity.service.PostSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 帖子搜索服务实现
 * ES 只做搜索引擎，返回 ID 后走数据库查完整 PostVO
 */
@Service
public class PostSearchServiceImpl implements PostSearchService {

    private static final Logger log = LoggerFactory.getLogger(PostSearchServiceImpl.class);
    private static final String INDEX_NAME = "chaocommunity_posts";

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private PostTagMapper postTagMapper;

    @Autowired
    private UserMapper userMapper;

    // ==================== 全量覆盖 / 新增 ====================

    @Override
    public void index(Post post) {
        try {
            PostDocument doc = buildDocument(post);
            esClient.index(IndexRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(post.getId()))
                    .document(doc)
            ));
        } catch (Exception e) {
            log.error("ES index post {} failed: {}", post.getId(), e.getMessage());
        }
    }

    // ==================== 删除 ====================

    @Override
    public void delete(Long postId) {
        try {
            esClient.delete(DeleteRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(postId))
            ));
        } catch (Exception e) {
            // 可能文档不存在，忽略
        }
    }

    // ==================== 局部更新 ====================

    @Override
    public void updateStatus(Long postId, Integer status) {
        try {
            esClient.update(UpdateRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(postId))
                    .doc(Map.of("status", status))
            ), PostDocument.class);
        } catch (Exception e) {
            log.error("ES updateStatus post {} failed: {}", postId, e.getMessage());
        }
    }

    @Override
    public void updateTop(Long postId, Integer isTop) {
        try {
            esClient.update(UpdateRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(postId))
                    .doc(Map.of("isTop", isTop))
            ), PostDocument.class);
        } catch (Exception e) {
            log.error("ES updateTop post {} failed: {}", postId, e.getMessage());
        }
    }

    // ==================== 计数器更新（script 原子增减，避免并发覆盖丢失） ====================

    @Override
    public void updateLikeCount(Long postId, int delta) {
        updateCountByScript(postId, "likeCount", delta);
    }

    @Override
    public void updateViewCount(Long postId, int delta) {
        updateCountByScript(postId, "viewCount", delta);
    }

    @Override
    public void updateCommentCount(Long postId, int delta) {
        updateCountByScript(postId, "commentCount", delta);
    }

    /**
     * script 原子增减：ctx._source.<field> += delta，由 ES 内部加锁串行执行
     */
    private void updateCountByScript(Long postId, String field, int delta) {
        try {
            esClient.update(UpdateRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(postId))
                    .script(s -> s
                            .lang("painless")
                            .source(src -> src.scriptString(
                                    "ctx._source." + field + " = Math.max(0, (ctx._source." + field + " == null ? 0 : ctx._source." + field + ") + params.delta)"
                            ))
                            .params("delta", co.elastic.clients.json.JsonData.of(delta))
                    )
            ), PostDocument.class);
        } catch (Exception e) {
            log.error("ES count update post {} field {} failed: {}", postId, field, e.getMessage());
        }
    }

    // ==================== 批量操作 ====================

    @Override
    public void batchHideByUserId(Long userId) {
        try {
            SearchResponse<PostDocument> response = esClient.search(SearchRequest.of(r -> r
                    .index(INDEX_NAME)
                    .query(q -> q.term(t -> t.field("authorId").value(userId)))
                    .size(1000)
                    .source(src -> src.fetch(false))
            ), PostDocument.class);
            for (Hit<PostDocument> hit : response.hits().hits()) {
                updateStatus(Long.valueOf(hit.id()), 1);
            }
        } catch (Exception e) {
            log.error("ES batchHideByUserId {} failed: {}", userId, e.getMessage());
        }
    }

    // ==================== 搜索 ====================

    @Override
    public PageResult<PostVO> search(String keyword, Long categoryId, String sort, boolean includeHidden, int page, int size) {
        int from = (page - 1) * size;

        SearchResponse<PostDocument> response;
        try {
            response = esClient.search(buildSearchRequest(keyword, categoryId, sort, includeHidden, from, size), PostDocument.class);
        } catch (Exception e) {
            log.error("ES search failed: {}", e.getMessage());
            return new PageResult<>(0, Collections.emptyList());
        }

        List<Long> orderedIds = response.hits().hits().stream()
                .map(Hit::id)
                .map(Long::valueOf)
                .collect(Collectors.toList());

        long total = response.hits().total() != null ? response.hits().total().value() : 0;

        if (orderedIds.isEmpty()) {
            return new PageResult<>(0, Collections.emptyList());
        }

        List<PostVO> postVOs = batchGetPostVOsOrdered(orderedIds);
        return new PageResult<>(total, postVOs);
    }

    // ==================== 全量同步 ====================

    /** 全量同步：将 DB 中所有未删除帖子写入 ES（含隐藏，搜索时通过 status 区分） */
    @Override
    public long fullSync() {
        List<Post> allPosts = postMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Post>()
                        .eq(Post::getDeleted, 0)
        );
        for (Post post : allPosts) {
            index(post);
        }
        return allPosts.size();
    }

    // ==================== 私有方法 ====================

    private PostDocument buildDocument(Post post) {
        String categoryName = null;
        if (post.getCategoryId() != null) {
            Category category = categoryMapper.selectById(post.getCategoryId());
            if (category != null) categoryName = category.getName();
        }

        List<String> tagNames = Collections.emptyList();
        List<PostTag> postTags = postTagMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PostTag>()
                        .eq(PostTag::getPostId, post.getId())
        );
        if (!postTags.isEmpty()) {
            List<Long> tagIds = postTags.stream().map(PostTag::getTagId).collect(Collectors.toList());
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            tagNames = tags.stream().map(Tag::getName).collect(Collectors.toList());
        }

        String username = null;
        User user = userMapper.selectById(post.getUserId());
        if (user != null) username = user.getUsername();

        return PostDocument.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categoryId(post.getCategoryId())
                .categoryName(categoryName)
                .tags(tagNames)
                .authorId(post.getUserId())
                .username(username)
                .createTime(post.getCreateTime())
                .updateTime(post.getUpdateTime())
                .viewCount(post.getViewCount() != null ? post.getViewCount() : 0)
                .likeCount(post.getLikeCount() != null ? post.getLikeCount() : 0)
                .commentCount(post.getCommentCount() != null ? post.getCommentCount() : 0)
                .isTop(post.getTop() != null ? post.getTop() : 0)
                .status(post.getStatus() != null ? post.getStatus() : 0)
                .build();
    }

    private SearchRequest buildSearchRequest(String keyword, Long categoryId, String sort, boolean includeHidden, int from, int size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        // 每次 .sort() 只能加一个排序条件（SortOptions 是 union type，复用会覆盖）
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .from(from)
                .size(size)
                .query(q -> q.bool(b -> {
                    if (hasKeyword) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("title^5", "content^3", "categoryName^2", "tags^1.5", "username")
                                .query(keyword)
                        ));
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                    }
                    if (!includeHidden) {
                        b.filter(f -> f.term(t -> t.field("status").value(0)));
                    }
                    if (categoryId != null) {
                        b.filter(f -> f.term(t -> t.field("categoryId").value(categoryId)));
                    }
                    return b;
                }));

        // 置顶永远最前
        builder.sort(s -> s.field(f -> f.field("isTop").order(SortOrder.Desc)));

        if ("hot".equals(sort)) {
            builder.sort(s -> s.field(f -> f.field("likeCount").order(SortOrder.Desc)));
            builder.sort(s -> s.field(f -> f.field("viewCount").order(SortOrder.Desc)));
            builder.sort(s -> s.field(f -> f.field("commentCount").order(SortOrder.Desc)));
        } else if ("newest".equals(sort)) {
            builder.sort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)));
        } else {
            builder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        return builder.build();
    }

    /** 批量查 DB 并保持 ES 返回的顺序 */
    private List<PostVO> batchGetPostVOsOrdered(List<Long> orderedIds) {
        List<PostVO> postVOs = postMapper.getPostVOsByIds(orderedIds);

        // 填充标签
        fillTags(postVOs);

        // 按 ES 顺序重排
        Map<Long, PostVO> idToVO = postVOs.stream()
                .collect(Collectors.toMap(PostVO::getId, vo -> vo, (a, b) -> a));
        return orderedIds.stream()
                .map(idToVO::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** 给 PostVO 列表批量填充标签 */
    private void fillTags(List<PostVO> voList) {
        if (voList.isEmpty()) return;

        List<Long> postIds = voList.stream().map(PostVO::getId).collect(Collectors.toList());
        List<PostTag> allPostTags = postTagMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PostTag>()
                        .in(PostTag::getPostId, postIds)
        );
        if (allPostTags.isEmpty()) return;

        List<Long> allTagIds = allPostTags.stream()
                .map(PostTag::getTagId).distinct().collect(Collectors.toList());
        List<Tag> allTags = tagMapper.selectBatchIds(allTagIds);
        Map<Long, Tag> tagMap = allTags.stream().collect(Collectors.toMap(Tag::getId, t -> t, (a, b) -> a));

        Map<Long, List<TagVO>> postTagsMap = new HashMap<>();
        for (PostTag pt : allPostTags) {
            Tag tag = tagMap.get(pt.getTagId());
            if (tag != null) {
                postTagsMap.computeIfAbsent(pt.getPostId(), k -> new ArrayList<>())
                        .add(TagVO.builder().id(tag.getId()).name(tag.getName()).icon(tag.getIcon()).build());
            }
        }

        for (PostVO vo : voList) {
            vo.setTags(postTagsMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }
    }
}
