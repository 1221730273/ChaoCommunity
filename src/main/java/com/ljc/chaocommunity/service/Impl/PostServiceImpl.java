package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.CategoryMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.mapper.PostTagMapper;
import com.ljc.chaocommunity.mapper.TagMapper;
import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.entity.Category;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.PostTag;
import com.ljc.chaocommunity.pojo.entity.Tag;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.pojo.vo.TagVO;
import com.ljc.chaocommunity.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private PostTagMapper postTagMapper;

    @Override
    @Transactional
    public Long createPost(PostDTO dto) {

        // 1. 校验分类是否存在
        Category category = categoryMapper.selectById(dto.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 2. 校验标签是否全部存在
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagMapper.selectBatchIds(dto.getTagIds());
            if (tags.size() != dto.getTagIds().size()) {
                throw new BusinessException("标签不存在");
            }
        }

        // 3. 保存帖子
        Post post = new Post();
        //TODO 引入springSecurity之后获取当前用户ID 现在硬编码为1
        post.setUserId(1L);
        post.setCategoryId(dto.getCategoryId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        //TODO 以后发帖子要审核 管理员身份可以审核
        post.setStatus(0);

        postMapper.insert(post);


        // 4. 批量保存帖子-标签关联（一条SQL插入所有关联）
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<PostTag> postTags = dto.getTagIds().stream()
                    .map(tagId -> {
                        PostTag pt = new PostTag();
                        pt.setPostId(post.getId());
                        pt.setTagId(tagId);
                        return pt;
                    })
                    .collect(Collectors.toList());
            postTagMapper.insertBatch(postTags);
        }

        return post.getId();
    }

    @Override
    public void deletePost(Long postId) {
        // TODO 校验当前用户是否为发帖人(从线程中获取当前的用户的id查询是否有这个帖子)或管理员
        postMapper.deleteById(postId);
    }

    @Override
    public void updatePost(PostDTO dto) {
        // TODO 修改也要审核 以后可能新增修改次数字段
        Post post = new Post();
        post.setId(dto.getId());
        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
        postMapper.updateById(post);
    }


    /**
     * 获取帖子详情
     *
     * @param postId 帖子ID
     * @return 帖子详情
     */
    @Override
    public PostVO getPostVOById(Long postId) {

        //TODO 标签是要带图标的 以后给标签表增加一个图片路径

        PostVO vo = postMapper.getPostVOById(postId);

        if (vo == null) {
            throw new BusinessException("帖子不存在");
        }
        if (vo.getStatus() == 1) {
            throw new BusinessException("帖子已经被隐藏");
        }

        // 根据 postId 查出所有 (post_id, tag_id) 记录
        LambdaQueryWrapper<PostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostTag::getPostId, postId);
        List<PostTag> postTags = postTagMapper.selectList(wrapper);


        // ===== 第三步：根据 tagId 列表去 tag 表批量查 =====
        List<TagVO> tagVOList = new ArrayList<>();
        if (!postTags.isEmpty()) {
            // 3a. 从 postTag 记录中提取出纯 tagId 列表：[3, 7, ...]
            List<Long> tagIds = postTags.stream()
                    .map(PostTag::getTagId)
                    .collect(Collectors.toList());

            // 3b. 批量查询 tag 表，一条 SQL：SELECT * FROM tag WHERE id IN (3, 7, ...)
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);

            // 3c. 将 Tag 实体 → TagVO（只返 id/name/icon，不返时间等敏感字段）
            tagVOList = tags.stream()
                    .map(tag -> TagVO.builder()
                            .id(tag.getId())
                            .name(tag.getName())
                            .icon(tag.getIcon())
                            .build())
                    .collect(Collectors.toList());
        }

        // 把标签列表塞进 PostVO
        vo.setTags(tagVOList);

        return vo;
    }

    /**
     * 首页分页查询：分类 + 排序（new/hot/follow）一条SQL搞定
     */
    @Override
    public PageResult<PostVO> pageQuery(PostPageQueryDTO dto) {

        // 1. 创建分页对象
        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());

        // 2. follow 模式传 currentUserId，new/hot 模式传 null
        //TODO 引入springSecurity之后从线程中获取当前用户ID
        Long currentUserId = "follow".equals(dto.getSort()) ? 1L : null;

        Page<PostVO> resultPage = postMapper.selectPageVo(page, dto.getCategoryId(), dto.getSort(), currentUserId);

        // 3. 给每条帖子填充标签
        fillTags(resultPage.getRecords());

        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 根据用户ID查询帖子（用户主页用）
     */
    @Override
    public PageResult<PostVO> getUserPosts(Long userId, PostPageQueryDTO dto) {

        // 1. 创建分页对象
        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());

        // 2. 分页查询
        Page<PostVO> resultPage = postMapper.selectPageVoByUserId(page, userId, dto.getSort());

        // 3. 给每条帖子填充标签
        fillTags(resultPage.getRecords());

        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 给帖子列表填充标签（pageQuery 和 getUserPosts 共用）
     */
    private void fillTags(List<PostVO> voList) {
        for (PostVO vo : voList) {
            LambdaQueryWrapper<PostTag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PostTag::getPostId, vo.getId());
            List<PostTag> postTags = postTagMapper.selectList(wrapper);

            List<TagVO> tagVOList = new ArrayList<>();
            if (!postTags.isEmpty()) {
                List<Long> tagIds = postTags.stream()
                        .map(PostTag::getTagId)
                        .collect(Collectors.toList());
                List<Tag> tags = tagMapper.selectBatchIds(tagIds);
                tagVOList = tags.stream()
                        .map(tag -> TagVO.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .icon(tag.getIcon())
                                .build())
                        .collect(Collectors.toList());
            }
            vo.setTags(tagVOList);
        }
    }
}
