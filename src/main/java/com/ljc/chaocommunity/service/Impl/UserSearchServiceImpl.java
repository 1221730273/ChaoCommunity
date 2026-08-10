package com.ljc.chaocommunity.service.Impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.ljc.chaocommunity.pojo.es.UserDocument;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import com.ljc.chaocommunity.service.UserSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Elasticsearch 用户搜索服务实现
 * 搜索直接走 ES，不查 DB
 */
@Service
public class UserSearchServiceImpl implements UserSearchService {

    private static final Logger log = LoggerFactory.getLogger(UserSearchServiceImpl.class);
    private static final String INDEX_NAME = "chaocommunity_users";

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private UserMapper userMapper;

    // ==================== 全量覆盖 / 新增 ====================

    @Override
    public void index(User user) {
        try {
            UserDocument doc = buildDocument(user);
            esClient.index(IndexRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(user.getId()))
                    .document(doc)
            ));
        } catch (Exception e) {
            log.error("ES index user {} failed: {}", user.getId(), e.getMessage());
        }
    }

    // ==================== 局部更新 ====================

    @Override
    public void updateStatus(Long userId, Integer status) {
        try {
            esClient.update(UpdateRequest.of(r -> r
                    .index(INDEX_NAME)
                    .id(String.valueOf(userId))
                    .doc(Map.of("status", status))
            ), UserDocument.class);
        } catch (Exception e) {
            log.error("ES updateStatus user {} failed: {}", userId, e.getMessage());
        }
    }

    // ==================== 搜索 ====================

    @Override
    public PageResult<UserVO> search(String keyword, boolean includeBanned, String sort, int page, int size) {
        int from = (page - 1) * size;

        SearchResponse<UserDocument> response;
        try {
            response = esClient.search(buildSearchRequest(keyword, includeBanned, sort, from, size), UserDocument.class);
        } catch (Exception e) {
            log.error("ES user search failed: {}", e.getMessage());
            return new PageResult<>(0, Collections.emptyList());
        }

        List<UserVO> voList = response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(this::toUserVO)
                .collect(Collectors.toList());

        long total = response.hits().total() != null ? response.hits().total().value() : 0;
        return new PageResult<>(total, voList);
    }

    // ==================== 全量同步 ====================

    @Override
    public long fullSync() {
        List<User> allUsers = userMapper.selectList(null);
        for (User user : allUsers) {
            index(user);
        }
        return allUsers.size();
    }

    // ==================== 私有方法 ====================

    private UserDocument buildDocument(User user) {
        return UserDocument.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .signature(user.getSignature())
                .followCount(user.getFollowCount() != null ? user.getFollowCount() : 0)
                .followerCount(user.getFollowerCount() != null ? user.getFollowerCount() : 0)
                .role(user.getRole() != null ? user.getRole() : 0)
                .status(user.getStatus() != null ? user.getStatus() : 0)
                .createTime(user.getCreateTime())
                .build();
    }

    private SearchRequest buildSearchRequest(String keyword, boolean includeBanned, String sort, int from, int size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();

        // 每次 .sort() 只能加一个排序条件（SortOptions 是 union type，复用会覆盖）
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(INDEX_NAME)
                .from(from)
                .size(size)
                .query(q -> q.bool(b -> {
                    if (hasKeyword) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .fields("username^3", "nickname^2", "signature")
                                .query(keyword)
                        ));
                    } else {
                        b.must(m -> m.matchAll(ma -> ma));
                    }
                    if (!includeBanned) {
                        b.filter(f -> f.term(t -> t.field("status").value(0)));
                    }
                    return b;
                }));

        if ("followers".equals(sort)) {
            builder.sort(s -> s.field(f -> f.field("followerCount").order(SortOrder.Desc)));
            builder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        } else {
            builder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }

        return builder.build();
    }

    private UserVO toUserVO(UserDocument doc) {
        UserVO vo = new UserVO();
        vo.setId(doc.getId());
        vo.setUsername(doc.getUsername());
        vo.setNickname(doc.getNickname());
        vo.setAvatar(doc.getAvatar());
        vo.setSignature(doc.getSignature());
        vo.setFollowCount(doc.getFollowCount());
        vo.setFollowerCount(doc.getFollowerCount());
        vo.setRole(doc.getRole());
        vo.setStatus(doc.getStatus());
        vo.setCreateTime(doc.getCreateTime());
        return vo;
    }
}
