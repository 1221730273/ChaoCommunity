package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.*;
import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.entity.*;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.pojo.vo.TagVO;
import com.ljc.chaocommunity.service.PostSearchService;
import com.ljc.chaocommunity.service.PostService;
import com.ljc.chaocommunity.util.SecurityUtils;
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

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Autowired
    private PostFileMapper postFileMapper;

    @Autowired
    private PostAuditMapper postAuditMapper;

    @Autowired
    private PostAuditFileMapper postAuditFileMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private PostSearchService postSearchService;


    /**
     * 创建帖子 → 提交审核（不直接插入 post 表）
     * @return 审核记录ID
     */
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

        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 3. 校验封面文件
        if (dto.getFileId() != null) {
            validateTempFile(dto.getFileId(), currentUserId);
        }

        // 4. 校验正文图片文件
        if (dto.getContentFileIds() != null && !dto.getContentFileIds().isEmpty()) {
            for (Long contentFileId : dto.getContentFileIds()) {
                validateTempFile(contentFileId, currentUserId);
            }
        }

        // 5. 创建审核记录（新帖，post_id=null, type=CREATE）
        PostAudit audit = new PostAudit();
        audit.setUserId(currentUserId);
        audit.setPostId(null);
        audit.setType("CREATE");
        audit.setTitle(dto.getTitle());
        audit.setContent(dto.getContent());
        audit.setCategoryId(dto.getCategoryId());
        audit.setCoverFileId(dto.getFileId());
        audit.setTagIds(dto.getTagIds() != null && !dto.getTagIds().isEmpty()
                ? dto.getTagIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                : null);
        audit.setStatus(0);
        postAuditMapper.insert(audit);

        // 6. 创建审核-文件关联记录
        List<PostAuditFile> auditFiles = new ArrayList<>();

        if (dto.getFileId() != null) {
            PostAuditFile coverAf = new PostAuditFile();
            coverAf.setAuditId(audit.getId());
            coverAf.setFileId(dto.getFileId());
            coverAf.setType("COVER");
            auditFiles.add(coverAf);
        }

        if (dto.getContentFileIds() != null && !dto.getContentFileIds().isEmpty()) {
            for (Long contentFileId : dto.getContentFileIds()) {
                PostAuditFile contentAf = new PostAuditFile();
                contentAf.setAuditId(audit.getId());
                contentAf.setFileId(contentFileId);
                contentAf.setType("CONTENT");
                auditFiles.add(contentAf);
            }
        }

        if (!auditFiles.isEmpty()) {
            postAuditFileMapper.insertBatch(auditFiles);
        }

        return audit.getId();
    }


    @Override
    @Transactional
    public void deletePost(Long postId) {
        // 校验当前用户是否为发帖人或管理员
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!post.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException("无权删除该帖子");
        }

        // 通过 post_file 关联表查找帖子关联的所有文件，将 file_record.status 置为 0
        LambdaQueryWrapper<PostFile> postFileWrapper = new LambdaQueryWrapper<>();
        postFileWrapper.eq(PostFile::getPostId, postId);
        List<PostFile> postFiles = postFileMapper.selectList(postFileWrapper);
        for (PostFile postFile : postFiles) {
            FileRecord fileRecord = fileRecordMapper.selectById(postFile.getFileId());
            if (fileRecord != null && fileRecord.getStatus() == 1) {
                fileRecord.setStatus(0);
                fileRecordMapper.updateById(fileRecord);
            }
        }

        // 级联软删除帖子下的所有评论
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getPostId, postId);
        commentMapper.delete(commentWrapper);

        postMapper.deleteById(postId);
        // ES 同步：删除
        postSearchService.delete(postId);
    }


    /**
     * 更新帖子 → 提交审核（不直接修改 post 表）
     * @return 审核记录ID
     */
    @Override
    @Transactional
    public Long updatePost(PostDTO dto) {
        // 1. 查询原帖子
        Post post = postMapper.selectById(dto.getId());
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // 校验是否本人或管理员
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!post.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException("无权修改该帖子");
        }

        // 2. 校验分类
        if (dto.getCategoryId() != null) {
            if (categoryMapper.selectById(dto.getCategoryId()) == null) {
                throw new BusinessException("分类不存在");
            }
        }

        // 3. 校验标签
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Tag> tags = tagMapper.selectBatchIds(dto.getTagIds());
            if (tags.size() != dto.getTagIds().size()) {
                throw new BusinessException("标签不存在");
            }
        }

        // 4. 检查是否有同帖子的待审核记录
        checkDuplicateAudit(post.getId(), "UPDATE");

        // 5. 校验封面文件（如果传了新的且不同于旧封面）
        Long oldCoverFileId = getOldCoverFileId(post.getId());
        if (dto.getFileId() != null && !dto.getFileId().equals(oldCoverFileId)) {
            validateTempFile(dto.getFileId(), currentUserId);
        }

        // 6. 校验正文图片文件（只校验新增的临时文件）
        if (dto.getContentFileIds() != null && !dto.getContentFileIds().isEmpty()) {
            List<Long> oldFileIds = getOldContentFileIds(post.getId());
            for (Long contentFileId : dto.getContentFileIds()) {
                if (!oldFileIds.contains(contentFileId)) {
                    validateTempFile(contentFileId, currentUserId);
                }
            }
        }

        // 7. 创建审核记录（type=UPDATE）
        PostAudit audit = new PostAudit();
        audit.setUserId(currentUserId);
        audit.setPostId(dto.getId());
        audit.setType("UPDATE");
        audit.setTitle(dto.getTitle());
        audit.setContent(dto.getContent());
        audit.setCategoryId(dto.getCategoryId());
        audit.setCoverFileId(dto.getFileId());
        audit.setTagIds(dto.getTagIds() != null && !dto.getTagIds().isEmpty()
                ? dto.getTagIds().stream().map(String::valueOf).collect(Collectors.joining(","))
                : null);
        audit.setStatus(0);
        postAuditMapper.insert(audit);

        // 7. 创建审核-文件关联记录
        List<PostAuditFile> auditFiles = new ArrayList<>();

        if (dto.getFileId() != null) {
            PostAuditFile coverAf = new PostAuditFile();
            coverAf.setAuditId(audit.getId());
            coverAf.setFileId(dto.getFileId());
            coverAf.setType("COVER");
            auditFiles.add(coverAf);
        }

        if (dto.getContentFileIds() != null && !dto.getContentFileIds().isEmpty()) {
            for (Long contentFileId : dto.getContentFileIds()) {
                PostAuditFile contentAf = new PostAuditFile();
                contentAf.setAuditId(audit.getId());
                contentAf.setFileId(contentFileId);
                contentAf.setType("CONTENT");
                auditFiles.add(contentAf);
            }
        }

        if (!auditFiles.isEmpty()) {
            postAuditFileMapper.insertBatch(auditFiles);
        }

        return audit.getId();
    }


    /**
     * 更新帖子封面 → 提交审核（只存封面数据，不存内容）
     * @return 审核记录ID
     */
    @Override
    @Transactional
    public Long updateCover(Long postId, CoverUpdateDTO dto) {
        // 1. 查询帖子
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        // 校验权限
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!post.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException("无权修改该帖子");
        }

        // 2. 检查是否有同帖子的待审核记录
        checkDuplicateAudit(postId, "COVER");

        // 3. 查询旧封面 file_id
        Long oldCoverFileId = getOldCoverFileId(postId);

        // 4. 新旧封面一致则直接返回
        if (dto.getFileId().equals(oldCoverFileId)) {
            throw new BusinessException("新封面与当前封面相同");
        }

        // 5. 校验新封面文件
        validateTempFile(dto.getFileId(), currentUserId);

        // 6. 创建审核记录（type=COVER，只存封面，不存内容）
        PostAudit audit = new PostAudit();
        audit.setUserId(currentUserId);
        audit.setPostId(postId);
        audit.setType("COVER");
        audit.setCoverFileId(dto.getFileId());
        audit.setTitle(post.getTitle());  // 仅用于管理端展示
        audit.setStatus(0);
        postAuditMapper.insert(audit);

        // 7. 创建审核-文件关联（仅封面）
        PostAuditFile coverAf = new PostAuditFile();
        coverAf.setAuditId(audit.getId());
        coverAf.setFileId(dto.getFileId());
        coverAf.setType("COVER");
        postAuditFileMapper.insert(coverAf);

        return audit.getId();
    }


    /**
     * 获取帖子详情
     */
    @Override
    public PostVO getPostVOById(Long postId) {

        PostVO vo = postMapper.getPostVOById(postId);

        if (vo == null) {
            throw new BusinessException("帖子不存在");
        }

        // 非公开帖子只有作者和管理员可见
        if (vo.getStatus() != 0) {
            Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
            if (currentUserId == null || (!vo.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin())) {
                throw new BusinessException("帖子不可见");
            }
        }

        // 查询标签
        LambdaQueryWrapper<PostTag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostTag::getPostId, postId);
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

        // 查询正文图片的 fileRecord ID 列表（用于前端编辑时回传）
        LambdaQueryWrapper<PostFile> pfWrapper = new LambdaQueryWrapper<>();
        pfWrapper.eq(PostFile::getPostId, postId)
                .eq(PostFile::getType, "CONTENT");
        List<PostFile> contentFiles = postFileMapper.selectList(pfWrapper);
        List<Long> contentFileIds = contentFiles.stream()
                .map(PostFile::getFileId)
                .collect(Collectors.toList());
        vo.setContentFileIds(contentFileIds);

        return vo;
    }


    /**
     * 首页分页查询：分类 + 排序（new/hot/follow）
     */
    @Override
    public PageResult<PostVO> pageQuery(PostPageQueryDTO dto) {
        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<PostVO> resultPage;
        switch (dto.getSort()) {
            case "hot":
                resultPage = postMapper.selectPageVoHot(page, dto.getCategoryId());
                break;
            case "follow":
                Long uid = SecurityUtils.getCurrentUserIdOrNull();
                if (uid == null) {
                    resultPage = postMapper.selectPageVoNewest(page, dto.getCategoryId());
                } else {
                    resultPage = postMapper.selectPageVoFollow(page, dto.getCategoryId(), uid);
                }
                break;
            default:
                resultPage = postMapper.selectPageVoNewest(page, dto.getCategoryId());
        }
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }


    /**
     * 根据用户ID查询帖子（查自己=全部状态，查别人=仅可见）
     */
    @Override
    public PageResult<PostVO> getUserPosts(Long userId, PostPageQueryDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        // 查自己时不过滤status，查别人只查可见帖子
        boolean includeAllStatus = currentUserId != null && currentUserId.equals(userId);

        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<PostVO> resultPage = "hot".equals(dto.getSort())
                ? postMapper.selectPageVoByUserIdHot(page, userId, includeAllStatus)
                : postMapper.selectPageVoByUserIdNewest(page, userId, includeAllStatus);
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }


    /**
     * 给帖子列表填充标签
     */
    @Override
    public void fillTags(List<PostVO> voList) {
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


    @Override
    @Transactional
    public void toggleFeatured(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        post.setIsFeatured(post.getIsFeatured() == 1 ? 0 : 1);
        postMapper.updateById(post);
        // ES 同步
        postSearchService.index(post);
    }

    @Override
    public PageResult<PostVO> pageQueryFeatured(int page, int size, String sort) {
        Page<PostVO> p = new Page<>(page, size);
        Page<PostVO> resultPage = "hot".equals(sort)
                ? postMapper.selectPageVoFeaturedHot(p)
                : postMapper.selectPageVoFeaturedNewest(p);
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 首页最新帖子：按创建时间倒序取 limit 条（绕过 MP 分页插件，直接 LIMIT）
     */
    @Override
    public List<PostVO> getLatestPosts(int limit) {
        // 直接查 post 表：status=0, deleted=0, 按 create_time 倒序（忽略置顶）, LIMIT
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 0)
                .eq(Post::getDeleted, 0)
                .orderByDesc(Post::getCreateTime)
                .last("LIMIT " + limit);
        List<Post> posts = postMapper.selectList(wrapper);
        List<PostVO> voList = posts.stream().map(post -> {
            PostVO vo = postMapper.getPostVOById(post.getId());
            return vo;
        }).collect(Collectors.toList());
        return voList;
    }

    /**
     * 首页最新精选帖子：is_featured=1 + status=0，按置顶优先 + 创建时间倒序取 limit 条
     */
    @Override
    public List<PostVO> getLatestFeatured(int limit) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getIsFeatured, 1)
                .eq(Post::getStatus, 0)
                .orderByDesc(Post::getCreateTime)
                .last("LIMIT " + limit);
        List<Post> posts = postMapper.selectList(wrapper);
        return posts.stream().map(post -> postMapper.getPostVOById(post.getId())).collect(Collectors.toList());
    }

    @Override
    public void incrementViewCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            int newCount = post.getViewCount() + 1;
            Post updatePost = new Post();
            updatePost.setId(postId);
            updatePost.setViewCount(newCount);
            postMapper.updateById(updatePost);
            // ES 同步
            postSearchService.updateViewCount(postId, newCount);
        }
    }


    // ==================== 用户自管理方法 ====================

    /**
     * 切换帖子隐藏状态（0↔1），本人或管理员
     */
    @Override
    @Transactional
    public void toggleHidePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!post.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException("无权操作该帖子");
        }
        // 切换：0→1 或 1→0
        int newStatus = post.getStatus() == 0 ? 1 : 0;
        post.setStatus(newStatus);
        postMapper.updateById(post);
        // ES 同步
        postSearchService.updateStatus(postId, newStatus);
    }

    /**
     * 查询自己的帖子（包括隐藏的，仅从post表）
     */
    @Override
    public PageResult<PostVO> getMyPosts(PostPageQueryDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<PostVO> resultPage = "hot".equals(dto.getSort())
                ? postMapper.selectPageVoByUserIdHot(page, currentUserId, true)
                : postMapper.selectPageVoByUserIdNewest(page, currentUserId, true);
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 查询自己的审核记录（包括新帖审核和更新审核，只返回审核中和审核失败）
     */
    @Override
    public List<PostAuditVO> getMyAudits() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<PostAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAudit::getUserId, currentUserId)
                .in(PostAudit::getStatus, 0, 2)  // 0=审核中 2=审核失败，排除已通过
                .orderByDesc(PostAudit::getCreateTime);
        List<PostAudit> audits = postAuditMapper.selectList(wrapper);

        List<PostAuditVO> voList = new ArrayList<>();
        for (PostAudit audit : audits) {
            PostAuditVO vo = new PostAuditVO();
            vo.setId(audit.getId());
            vo.setUserId(audit.getUserId());
            vo.setType(audit.getType());
            vo.setPostId(audit.getPostId());
            vo.setTitle(audit.getTitle());
            vo.setContent(audit.getContent());
            vo.setCategoryId(audit.getCategoryId());
            vo.setCoverFileId(audit.getCoverFileId());
            vo.setTagIds(audit.getTagIds());
            vo.setStatus(audit.getStatus());
            vo.setRejectReason(audit.getRejectReason());
            vo.setCreateTime(audit.getCreateTime());

            // 用户信息
            User user = userMapper.selectById(audit.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }

            // 分类名称
            if (audit.getCategoryId() != null) {
                Category category = categoryMapper.selectById(audit.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                }
            }

            // 封面URL
            if (audit.getCoverFileId() != null) {
                FileRecord coverFile = fileRecordMapper.selectById(audit.getCoverFileId());
                if (coverFile != null) {
                    vo.setCoverUrl(coverFile.getUrl());
                }
            }

            // 正文图片fileId列表
            LambdaQueryWrapper<PostAuditFile> fileWrapper = new LambdaQueryWrapper<>();
            fileWrapper.eq(PostAuditFile::getAuditId, audit.getId())
                    .eq(PostAuditFile::getType, "CONTENT");
            List<PostAuditFile> auditFiles = postAuditFileMapper.selectList(fileWrapper);
            List<Long> contentFileIds = auditFiles.stream()
                    .map(PostAuditFile::getFileId)
                    .collect(Collectors.toList());
            vo.setContentFileIds(contentFileIds);

            voList.add(vo);
        }
        return voList;
    }

    /**
     * 分页查询所有帖子（管理端，包含隐藏）
     */
    @Override
    public PageResult<PostVO> pageQueryAll(PostPageQueryDTO dto) {
        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<PostVO> resultPage = "hot".equals(dto.getSort())
                ? postMapper.selectPageVoAllHot(page, dto.getCategoryId())
                : postMapper.selectPageVoAllNewest(page, dto.getCategoryId());
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 删除帖子（管理端，不限本人）
     */
    @Override
    @Transactional
    public void adminDeletePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }

        LambdaQueryWrapper<PostFile> postFileWrapper = new LambdaQueryWrapper<>();
        postFileWrapper.eq(PostFile::getPostId, postId);
        List<PostFile> postFiles = postFileMapper.selectList(postFileWrapper);
        for (PostFile pf : postFiles) {
            FileRecord fr = fileRecordMapper.selectById(pf.getFileId());
            if (fr != null && fr.getStatus() == 1) {
                fr.setStatus(0);
                fileRecordMapper.updateById(fr);
            }
        }

        // 级联软删除帖子下的所有评论
        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getPostId, postId);
        commentMapper.delete(commentWrapper);

        postMapper.deleteById(postId);
        // ES 同步：删除
        postSearchService.delete(postId);
    }

    // ==================== 置顶管理 ====================

    @Override
    @Transactional
    public void toggleTop(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        int newTop = post.getTop() == 1 ? 0 : 1;
        post.setTop(newTop);
        postMapper.updateById(post);
        // ES 同步
        postSearchService.updateTop(postId, newTop);
    }

    // ==================== 管理员隐藏用户帖子 ====================

    @Override
    @Transactional
    public int adminHideUserPosts(Long userId) {
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post> uw =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        uw.eq(Post::getUserId, userId)
          .eq(Post::getStatus, 0)
          .set(Post::getStatus, 1);
        int rows = postMapper.update(null, uw);
        // ES 同步
        postSearchService.batchHideByUserId(userId);
        return rows;
    }

    /**
     * 删除自己审核失败的记录（status=2）
     */
    @Override
    @Transactional
    public void deleteFailedAudit(Long auditId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        PostAudit audit = postAuditMapper.selectById(auditId);
        if (audit == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (!audit.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权操作");
        }
        if (audit.getStatus() != 2) {
            throw new BusinessException("只能删除审核失败的记录");
        }

        // 物理删除审核记录和关联的文件记录
        LambdaQueryWrapper<PostAuditFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(PostAuditFile::getAuditId, auditId);
        postAuditFileMapper.delete(fileWrapper);

        postAuditMapper.deleteById(auditId);
    }


    // ==================== 私有辅助方法 ====================

    /**
     * 检查同帖子是否已有待审核记录
     */
    private void checkDuplicateAudit(Long postId, String type) {
        LambdaQueryWrapper<PostAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAudit::getPostId, postId)
                .eq(PostAudit::getType, type)
                .eq(PostAudit::getStatus, 0);
        if (postAuditMapper.selectCount(wrapper) > 0) {
            String typeLabel = "COVER".equals(type) ? "封面" : "内容";
            throw new BusinessException("该帖子已有" + typeLabel + "修改在审核中，请等待审核完成");
        }
    }

    /**
     * 校验临时文件：存在、属于当前用户、未使用、在 temp/ 目录下
     */
    private void validateTempFile(Long fileId, Long userId) {
        FileRecord fileRecord = fileRecordMapper.selectById(fileId);
        if (fileRecord == null) {
            throw new BusinessException("文件不存在");
        }
        if (!fileRecord.getUserId().equals(userId)) {
            throw new BusinessException("无权使用该文件");
        }
        if (fileRecord.getStatus() == 1) {
            throw new BusinessException("文件已经被使用");
        }
        if (!fileRecord.getFilePath().startsWith("temp/")) {
            throw new BusinessException("非法文件");
        }
    }

    /**
     * 查询帖子的旧封面 file_id
     */
    private Long getOldCoverFileId(Long postId) {
        LambdaQueryWrapper<PostFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFile::getPostId, postId)
                .eq(PostFile::getType, "COVER");
        PostFile pf = postFileMapper.selectOne(wrapper);
        return pf != null ? pf.getFileId() : null;
    }

    /**
     * 查询帖子的旧正文图片 file_id 集合
     */
    private List<Long> getOldContentFileIds(Long postId) {
        LambdaQueryWrapper<PostFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFile::getPostId, postId)
                .eq(PostFile::getType, "CONTENT");
        return postFileMapper.selectList(wrapper).stream()
                .map(PostFile::getFileId)
                .collect(Collectors.toList());
    }
}
