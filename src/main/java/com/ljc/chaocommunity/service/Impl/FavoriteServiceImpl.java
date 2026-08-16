package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.PostFavoriteMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.PostFavorite;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.service.FavoriteService;
import com.ljc.chaocommunity.service.PostService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private PostFavoriteMapper postFavoriteMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostService postService;

    @Override
    @Transactional
    public void favorite(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // 隐藏帖子仅作者本人可收藏
        if (post.getStatus() != null && post.getStatus() != 0 && !post.getUserId().equals(currentUserId)) {
            throw new BusinessException("帖子不可见");
        }

        // 检查是否已收藏（唯一索引兜底，selectCount 给友好提示）
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getUserId, currentUserId)
                .eq(PostFavorite::getPostId, postId);
        if (postFavoriteMapper.selectCount(wrapper) > 0) {
            return;
        }

        PostFavorite pf = new PostFavorite();
        pf.setUserId(currentUserId);
        pf.setPostId(postId);
        postFavoriteMapper.insert(pf);
    }

    @Override
    @Transactional
    public void unfavorite(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getUserId, currentUserId)
                .eq(PostFavorite::getPostId, postId);
        PostFavorite pf = postFavoriteMapper.selectOne(wrapper);
        if (pf == null) {
            return;
        }

        postFavoriteMapper.deleteById(pf.getId());
    }

    @Override
    public boolean isFavorited(Long postId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getUserId, currentUserId)
                .eq(PostFavorite::getPostId, postId);
        return postFavoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public java.util.Map<Long, Boolean> isFavoritedBatch(java.util.List<Long> postIds) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        java.util.Map<Long, Boolean> result = new java.util.HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getUserId, currentUserId)
                .in(PostFavorite::getPostId, postIds);
        java.util.Set<Long> favoritedIds = postFavoriteMapper.selectList(wrapper).stream()
                .map(PostFavorite::getPostId)
                .collect(Collectors.toSet());
        for (Long postId : postIds) {
            result.put(postId, favoritedIds.contains(postId));
        }
        return result;
    }

    @Override
    public PageResult<PostVO> myFavorites(int page, int size) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 分页查收藏记录
        Page<PostFavorite> p = new Page<>(page, size);
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getUserId, currentUserId)
                .orderByDesc(PostFavorite::getCreateTime);
        Page<PostFavorite> resultPage = postFavoriteMapper.selectPage(p, wrapper);

        List<Long> postIds = resultPage.getRecords().stream()
                .map(PostFavorite::getPostId)
                .collect(Collectors.toList());

        if (postIds.isEmpty()) {
            return new PageResult<>(0L, List.of());
        }

        // 批量查 PostVO + 填标签
        List<PostVO> voList = postMapper.getPostVOsByIds(postIds);
        postService.fillTags(voList);

        return new PageResult<>(resultPage.getTotal(), voList);
    }
}
