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

        // 5. 创建审核记录（新帖，post_id=null）
        PostAudit audit = new PostAudit();
        audit.setUserId(currentUserId);
        audit.setPostId(null);
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

        postMapper.deleteById(postId);
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

        // 4. 校验封面文件（如果传了新的且不同于旧封面）
        Long oldCoverFileId = getOldCoverFileId(post.getId());
        if (dto.getFileId() != null && !dto.getFileId().equals(oldCoverFileId)) {
            validateTempFile(dto.getFileId(), currentUserId);
        }

        // 5. 校验正文图片文件（只校验新增的临时文件）
        if (dto.getContentFileIds() != null && !dto.getContentFileIds().isEmpty()) {
            List<Long> oldFileIds = getOldContentFileIds(post.getId());
            for (Long contentFileId : dto.getContentFileIds()) {
                // 只校验新增的临时文件（不在旧集合中的）
                if (!oldFileIds.contains(contentFileId)) {
                    validateTempFile(contentFileId, currentUserId);
                }
            }
        }

        // 6. 创建审核记录
        PostAudit audit = new PostAudit();
        audit.setUserId(currentUserId);
        audit.setPostId(dto.getId()); // 关联原帖子
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
     * 更新帖子封面 → 提交审核
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

        // 2. 查询旧封面 file_id
        Long oldCoverFileId = getOldCoverFileId(postId);

        // 3. 新旧封面一致则直接返回
        if (dto.getFileId().equals(oldCoverFileId)) {
            throw new BusinessException("新封面与当前封面相同");
        }

        // 4. 校验新封面文件
        validateTempFile(dto.getFileId(), currentUserId);

        // 5. 查询旧标签（用于审核记录）
        LambdaQueryWrapper<PostTag> tagWrapper = new LambdaQueryWrapper<>();
        tagWrapper.eq(PostTag::getPostId, post.getId());
        List<PostTag> postTags = postTagMapper.selectList(tagWrapper);
        String tagIds = postTags.stream()
                .map(pt -> String.valueOf(pt.getTagId()))
                .collect(Collectors.joining(","));

        // 6. 创建审核记录
        PostAudit audit = new PostAudit();
        audit.setUserId(currentUserId);
        audit.setPostId(postId);
        audit.setTitle(post.getTitle());
        audit.setContent(post.getContent());
        audit.setCategoryId(post.getCategoryId());
        audit.setCoverFileId(dto.getFileId()); // 新封面
        audit.setTagIds(tagIds.isEmpty() ? null : tagIds);
        audit.setStatus(0);
        postAuditMapper.insert(audit);

        // 7. 创建审核-文件关联记录（新封面 + 保留旧正文图片）
        List<PostAuditFile> auditFiles = new ArrayList<>();

        // 新封面
        PostAuditFile coverAf = new PostAuditFile();
        coverAf.setAuditId(audit.getId());
        coverAf.setFileId(dto.getFileId());
        coverAf.setType("COVER");
        auditFiles.add(coverAf);

        // 保留旧的正文图片
        List<Long> oldContentFileIds = getOldContentFileIds(postId);
        for (Long fileId : oldContentFileIds) {
            PostAuditFile contentAf = new PostAuditFile();
            contentAf.setAuditId(audit.getId());
            contentAf.setFileId(fileId);
            contentAf.setType("CONTENT");
            auditFiles.add(contentAf);
        }

        postAuditFileMapper.insertBatch(auditFiles);

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
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (!vo.getUserId().equals(currentUserId) && !SecurityUtils.isAdmin()) {
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
        Long currentUserId = "follow".equals(dto.getSort()) ? SecurityUtils.getCurrentUserId() : null;
        Page<PostVO> resultPage = postMapper.selectPageVo(page, dto.getCategoryId(), dto.getSort(), currentUserId);
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }


    /**
     * 根据用户ID查询帖子（查自己=全部状态，查别人=仅可见）
     */
    @Override
    public PageResult<PostVO> getUserPosts(Long userId, PostPageQueryDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        // 查自己时不过滤status，查别人只查可见帖子
        boolean includeAllStatus = currentUserId.equals(userId);

        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<PostVO> resultPage = postMapper.selectPageVoByUserId(page, userId, dto.getSort(), includeAllStatus);
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }


    /**
     * 给帖子列表填充标签
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


    @Override
    public void incrementViewCount(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post != null) {
            Post updatePost = new Post();
            updatePost.setId(postId);
            updatePost.setViewCount(post.getViewCount() + 1);
            postMapper.updateById(updatePost);
        }
    }


    // ==================== 用户自管理方法 ====================

    /**
     * 切换帖子隐藏状态（0↔1），仅本人
     */
    @Override
    @Transactional
    public void toggleHidePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!post.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权操作该帖子");
        }
        // 切换：0→1 或 1→0
        post.setStatus(post.getStatus() == 0 ? 1 : 0);
        postMapper.updateById(post);
    }

    /**
     * 查询自己的帖子（包括隐藏的，仅从post表）
     */
    @Override
    public PageResult<PostVO> getMyPosts(PostPageQueryDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<PostVO> page = new Page<>(dto.getPage(), dto.getSize());
        Page<PostVO> resultPage = postMapper.selectPageVoByUserId(page, currentUserId, dto.getSort(), true);
        fillTags(resultPage.getRecords());
        return new PageResult<>(resultPage.getTotal(), resultPage.getRecords());
    }

    /**
     * 查询自己的审核记录（post_audit中post_id为null的新帖审核）
     */
    @Override
    public List<PostAuditVO> getMyAudits() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        LambdaQueryWrapper<PostAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAudit::getUserId, currentUserId)
                .isNull(PostAudit::getPostId)
                .orderByDesc(PostAudit::getCreateTime);
        List<PostAudit> audits = postAuditMapper.selectList(wrapper);

        List<PostAuditVO> voList = new ArrayList<>();
        for (PostAudit audit : audits) {
            PostAuditVO vo = new PostAuditVO();
            vo.setId(audit.getId());
            vo.setUserId(audit.getUserId());
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
