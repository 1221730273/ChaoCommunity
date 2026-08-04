package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.*;
import com.ljc.chaocommunity.pojo.entity.*;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.service.PostAuditService;
import com.ljc.chaocommunity.util.OssUtil;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostAuditServiceImpl implements PostAuditService {

    @Autowired
    private PostAuditMapper postAuditMapper;

    @Autowired
    private PostAuditFileMapper postAuditFileMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostFileMapper postFileMapper;

    @Autowired
    private PostTagMapper postTagMapper;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private OssUtil ossUtil;

    // ==================== 管理端：审核列表 ====================

    @Override
    public PageResult<PostAuditVO> getAuditList(Integer status, int page, int size) {
        LambdaQueryWrapper<PostAudit> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(PostAudit::getStatus, status);
        }
        wrapper.orderByDesc(PostAudit::getCreateTime);

        Page<PostAudit> p = new Page<>(page, size);
        Page<PostAudit> resultPage = postAuditMapper.selectPage(p, wrapper);

        List<PostAuditVO> voList = resultPage.getRecords().stream()
                .map(this::toAuditVO)
                .collect(Collectors.toList());
        return new PageResult<>(resultPage.getTotal(), voList);
    }

    // ==================== 管理端：删除审核 ====================

    @Override
    @Transactional
    public void deleteAudit(Long auditId) {
        PostAudit audit = postAuditMapper.selectById(auditId);
        if (audit == null) {
            throw new BusinessException("审核记录不存在");
        }

        // 删除关联的审核文件记录
        LambdaQueryWrapper<PostAuditFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(PostAuditFile::getAuditId, auditId);
        List<PostAuditFile> auditFiles = postAuditFileMapper.selectList(fileWrapper);
        // 释放关联的临时文件
        for (PostAuditFile af : auditFiles) {
            FileRecord fr = fileRecordMapper.selectById(af.getFileId());
            if (fr != null && fr.getStatus() == 0) {
                fr.setStatus(0);
                fileRecordMapper.updateById(fr);
            }
        }
        postAuditFileMapper.delete(fileWrapper);
        postAuditMapper.deleteById(auditId);
    }

    /** PostAudit → PostAuditVO 通用转换 */
    private PostAuditVO toAuditVO(PostAudit audit) {
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
        vo.setHandlerId(audit.getHandlerId());
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

        // 正文图片
        LambdaQueryWrapper<PostAuditFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(PostAuditFile::getAuditId, audit.getId())
                .eq(PostAuditFile::getType, "CONTENT");
        List<PostAuditFile> auditFiles = postAuditFileMapper.selectList(fileWrapper);
        List<Long> contentFileIds = auditFiles.stream()
                .map(PostAuditFile::getFileId)
                .collect(Collectors.toList());
        vo.setContentFileIds(contentFileIds);
        if (!contentFileIds.isEmpty()) {
            List<String> contentFileUrls = fileRecordMapper.selectBatchIds(contentFileIds).stream()
                    .map(FileRecord::getUrl)
                    .collect(Collectors.toList());
            vo.setContentFileUrls(contentFileUrls);
        }

        return vo;
    }

    // ==================== 审核通过 ====================

    @Override
    @Transactional
    public void approveAudit(Long auditId) {
        Long handlerId = SecurityUtils.getCurrentUserId();

        PostAudit audit = postAuditMapper.selectById(auditId);
        if (audit == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (audit.getStatus() != 0) {
            throw new BusinessException("该审核已处理过");
        }

        switch (audit.getType()) {
            case "CREATE":
                createPostFromAudit(audit);
                break;
            case "UPDATE":
                updatePostFromAudit(audit);
                break;
            case "COVER":
                updateCoverFromAudit(audit);
                break;
            default:
                throw new BusinessException("不支持的审核类型");
        }

        // 更新审核状态
        audit.setStatus(1);
        audit.setHandlerId(handlerId);
        postAuditMapper.updateById(audit);
    }

    // ==================== 审核拒绝 ====================

    @Override
    @Transactional
    public void rejectAudit(Long auditId, String reason) {
        Long handlerId = SecurityUtils.getCurrentUserId();

        PostAudit audit = postAuditMapper.selectById(auditId);
        if (audit == null) {
            throw new BusinessException("审核记录不存在");
        }
        if (audit.getStatus() != 0) {
            throw new BusinessException("该审核已处理过");
        }

        audit.setStatus(2);
        audit.setRejectReason(reason);
        audit.setHandlerId(handlerId);
        postAuditMapper.updateById(audit);
    }

    /**
     * 审核通过 — 仅更新封面
     */
    private void updateCoverFromAudit(PostAudit audit) {
        Post post = postMapper.selectById(audit.getPostId());
        if (post == null) {
            throw new BusinessException("原帖子不存在");
        }

        // 1. 查询旧封面
        LambdaQueryWrapper<PostFile> pfWrapper = new LambdaQueryWrapper<>();
        pfWrapper.eq(PostFile::getPostId, post.getId())
                .eq(PostFile::getType, "COVER");
        PostFile oldCover = postFileMapper.selectOne(pfWrapper);

        Long newCoverFileId = audit.getCoverFileId();
        if (newCoverFileId == null) {
            return; // 没有新封面，不处理
        }

        // 2. 移动新封面文件
        FileRecord newFr = fileRecordMapper.selectById(newCoverFileId);
        if (newFr == null) throw new BusinessException("封面文件不存在");

        String oldKey = newFr.getFilePath();
        String newKey = oldKey.replace("temp/", "post/cover/");
        OssUtil.UploadResult moveResult = ossUtil.move(oldKey, newKey);

        newFr.setFilePath(moveResult.objectKey());
        newFr.setUrl(moveResult.url());
        newFr.setStatus(1);
        fileRecordMapper.updateById(newFr);

        // 3. 释放旧封面
        if (oldCover != null) {
            FileRecord oldFr = fileRecordMapper.selectById(oldCover.getFileId());
            if (oldFr != null && oldFr.getStatus() == 1) {
                oldFr.setStatus(0);
                fileRecordMapper.updateById(oldFr);
            }
            postFileMapper.deleteById(oldCover.getId());
        }

        // 4. 插入新封面 post_file
        PostFile newPf = new PostFile();
        newPf.setPostId(post.getId());
        newPf.setFileId(newFr.getId());
        newPf.setType("COVER");
        postFileMapper.insert(newPf);

        // 5. 更新帖子 coverUrl（不动内容）
        post.setCoverUrl(moveResult.url());
        postMapper.updateById(post);
    }

    // ==================== 根据用户ID查询审核 ====================

    @Override
    public List<PostAuditVO> getAuditListByUserId(Long userId) {
        LambdaQueryWrapper<PostAudit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostAudit::getUserId, userId)
                .orderByDesc(PostAudit::getCreateTime);
        List<PostAudit> audits = postAuditMapper.selectList(wrapper);
        return audits.stream().map(this::toAuditVO).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    @Autowired
    private UserMapper userMapper;

    /**
     * 从审核记录创建新帖子（审核新帖通过）
     */
    private void createPostFromAudit(PostAudit audit) {
        // 1. 查询审核关联的所有文件
        LambdaQueryWrapper<PostAuditFile> fileWrapper = new LambdaQueryWrapper<>();
        fileWrapper.eq(PostAuditFile::getAuditId, audit.getId());
        List<PostAuditFile> auditFiles = postAuditFileMapper.selectList(fileWrapper);

        // 2. 处理封面文件：temp/ → post/cover/
        String coverUrl = null;
        Long coverFileRecordId = null;
        for (PostAuditFile af : auditFiles) {
            if ("COVER".equals(af.getType())) {
                FileRecord fr = fileRecordMapper.selectById(af.getFileId());
                if (fr == null) throw new BusinessException("封面文件不存在");

                String oldKey = fr.getFilePath();
                String newKey = oldKey.replace("temp/", "post/cover/");
                OssUtil.UploadResult moveResult = ossUtil.move(oldKey, newKey);

                fr.setFilePath(moveResult.objectKey());
                fr.setUrl(moveResult.url());
                fr.setStatus(1);
                fileRecordMapper.updateById(fr);

                coverUrl = moveResult.url();
                coverFileRecordId = fr.getId();
                break; // 只有一个封面
            }
        }

        // 3. 创建帖子
        Post post = new Post();
        post.setUserId(audit.getUserId());
        post.setCategoryId(audit.getCategoryId());
        post.setTitle(audit.getTitle());
        post.setContent(audit.getContent());
        post.setCoverUrl(coverUrl);
        post.setStatus(0); // 审核通过，展示
        postMapper.insert(post);

        // 4. 处理正文图片：temp/ → post/content/，替换 Markdown URL
        String content = post.getContent();
        for (PostAuditFile af : auditFiles) {
            if ("CONTENT".equals(af.getType())) {
                FileRecord fr = fileRecordMapper.selectById(af.getFileId());
                if (fr == null) throw new BusinessException("正文图片文件不存在");

                String oldUrl = fr.getUrl();
                String oldKey = fr.getFilePath();
                String newKey = oldKey.replace("temp/", "post/content/");
                OssUtil.UploadResult moveResult = ossUtil.move(oldKey, newKey);

                // 替换 content 中的临时URL
                content = content.replace(oldUrl, moveResult.url());

                fr.setFilePath(moveResult.objectKey());
                fr.setUrl(moveResult.url());
                fr.setStatus(1);
                fileRecordMapper.updateById(fr);

                // 写入 post_file
                PostFile pf = new PostFile();
                pf.setPostId(post.getId());
                pf.setFileId(fr.getId());
                pf.setType("CONTENT");
                postFileMapper.insert(pf);
            }
        }
        // 更新替换后的 content
        if (!content.equals(post.getContent())) {
            post.setContent(content);
            postMapper.updateById(post);
        }

        // 5. 封面 post_file
        if (coverFileRecordId != null) {
            PostFile pf = new PostFile();
            pf.setPostId(post.getId());
            pf.setFileId(coverFileRecordId);
            pf.setType("COVER");
            postFileMapper.insert(pf);
        }

        // 6. 处理标签
        if (audit.getTagIds() != null && !audit.getTagIds().isEmpty()) {
            List<PostTag> postTags = Arrays.stream(audit.getTagIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(tagIdStr -> {
                        PostTag pt = new PostTag();
                        pt.setPostId(post.getId());
                        pt.setTagId(Long.valueOf(tagIdStr));
                        return pt;
                    })
                    .collect(Collectors.toList());
            if (!postTags.isEmpty()) {
                postTagMapper.insertBatch(postTags);
            }
        }
    }

    /**
     * 从审核记录更新已有帖子（审核修改通过）
     */
    private void updatePostFromAudit(PostAudit audit) {
        Post post = postMapper.selectById(audit.getPostId());
        if (post == null) {
            throw new BusinessException("原帖子不存在");
        }

        // 1. 查询审核关联的所有文件
        LambdaQueryWrapper<PostAuditFile> auditFileWrapper = new LambdaQueryWrapper<>();
        auditFileWrapper.eq(PostAuditFile::getAuditId, audit.getId());
        List<PostAuditFile> auditFiles = postAuditFileMapper.selectList(auditFileWrapper);

        // 2. 查询旧的 post_file
        LambdaQueryWrapper<PostFile> postFileWrapper = new LambdaQueryWrapper<>();
        postFileWrapper.eq(PostFile::getPostId, post.getId());
        List<PostFile> oldPostFiles = postFileMapper.selectList(postFileWrapper);

        // ===== 3. 处理封面 diff =====
        PostFile oldCover = oldPostFiles.stream()
                .filter(pf -> "COVER".equals(pf.getType()))
                .findFirst().orElse(null);

        Long newCoverFileId = audit.getCoverFileId();
        Long oldCoverFileId = oldCover != null ? oldCover.getFileId() : null;

        String newCoverUrl = null;
        if (newCoverFileId != null && !newCoverFileId.equals(oldCoverFileId)) {
            // 封面发生了变化
            FileRecord newFr = fileRecordMapper.selectById(newCoverFileId);
            if (newFr == null) throw new BusinessException("封面文件不存在");

            String oldKey = newFr.getFilePath();
            String newKey = oldKey.replace("temp/", "post/cover/");
            OssUtil.UploadResult moveResult = ossUtil.move(oldKey, newKey);

            newFr.setFilePath(moveResult.objectKey());
            newFr.setUrl(moveResult.url());
            newFr.setStatus(1);
            fileRecordMapper.updateById(newFr);

            newCoverUrl = moveResult.url();

            // 释放旧封面
            if (oldCover != null) {
                FileRecord oldFr = fileRecordMapper.selectById(oldCover.getFileId());
                if (oldFr != null && oldFr.getStatus() == 1) {
                    oldFr.setStatus(0);
                    fileRecordMapper.updateById(oldFr);
                }
                postFileMapper.deleteById(oldCover.getId());
            }

            // 插入新的封面 post_file
            PostFile newPf = new PostFile();
            newPf.setPostId(post.getId());
            newPf.setFileId(newFr.getId());
            newPf.setType("COVER");
            postFileMapper.insert(newPf);
        } else if (newCoverFileId != null && newCoverFileId.equals(oldCoverFileId)) {
            // 封面未变，保持原样
            if (oldCover != null) {
                FileRecord fr = fileRecordMapper.selectById(oldCover.getFileId());
                if (fr != null) {
                    newCoverUrl = fr.getUrl();
                }
            }
        }

        // ===== 4. 处理正文图片 diff =====
        List<Long> oldContentFileIds = oldPostFiles.stream()
                .filter(pf -> "CONTENT".equals(pf.getType()))
                .map(PostFile::getFileId)
                .collect(Collectors.toList());

        List<Long> newContentFileIds = auditFiles.stream()
                .filter(af -> "CONTENT".equals(af.getType()))
                .map(PostAuditFile::getFileId)
                .collect(Collectors.toList());

        // 被删除的：old - new
        List<Long> toDelete = oldContentFileIds.stream()
                .filter(id -> !newContentFileIds.contains(id))
                .collect(Collectors.toList());

        // 新增的：new - old
        List<Long> toAdd = newContentFileIds.stream()
                .filter(id -> !oldContentFileIds.contains(id))
                .collect(Collectors.toList());

        // 处理删除：file_record.status=0，删除 post_file
        for (Long fileId : toDelete) {
            FileRecord fr = fileRecordMapper.selectById(fileId);
            if (fr != null && fr.getStatus() == 1) {
                fr.setStatus(0);
                fileRecordMapper.updateById(fr);
            }
            LambdaQueryWrapper<PostFile> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(PostFile::getPostId, post.getId())
                    .eq(PostFile::getFileId, fileId);
            postFileMapper.delete(deleteWrapper);
        }

        // 处理新增：移动文件 + 替换URL + 插入 post_file
        String content = audit.getContent();
        for (Long fileId : toAdd) {
            FileRecord fr = fileRecordMapper.selectById(fileId);
            if (fr == null) throw new BusinessException("正文图片文件不存在，fileId=" + fileId);

            String oldUrl = fr.getUrl();
            String oldKey = fr.getFilePath();
            String newKey = oldKey.replace("temp/", "post/content/");
            OssUtil.UploadResult moveResult = ossUtil.move(oldKey, newKey);

            // 替换 content 中的临时URL为正式URL
            content = content.replace(oldUrl, moveResult.url());

            fr.setFilePath(moveResult.objectKey());
            fr.setUrl(moveResult.url());
            fr.setStatus(1);
            fileRecordMapper.updateById(fr);

            PostFile pf = new PostFile();
            pf.setPostId(post.getId());
            pf.setFileId(fr.getId());
            pf.setType("CONTENT");
            postFileMapper.insert(pf);
        }

        // ===== 5. 更新帖子 =====
        post.setTitle(audit.getTitle());
        post.setContent(content);
        if (audit.getCategoryId() != null) {
            post.setCategoryId(audit.getCategoryId());
        }
        if (newCoverUrl != null) {
            post.setCoverUrl(newCoverUrl);
        }
        // 保持原有 status（不改变可见性）
        postMapper.updateById(post);

        // ===== 6. 更新标签 =====
        if (audit.getTagIds() != null) {
            // 删除旧标签关联
            LambdaQueryWrapper<PostTag> tagWrapper = new LambdaQueryWrapper<>();
            tagWrapper.eq(PostTag::getPostId, post.getId());
            postTagMapper.delete(tagWrapper);

            // 插入新标签关联
            if (!audit.getTagIds().isEmpty()) {
                List<PostTag> postTags = Arrays.stream(audit.getTagIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(tagIdStr -> {
                            PostTag pt = new PostTag();
                            pt.setPostId(post.getId());
                            pt.setTagId(Long.valueOf(tagIdStr));
                            return pt;
                        })
                        .collect(Collectors.toList());
                if (!postTags.isEmpty()) {
                    postTagMapper.insertBatch(postTags);
                }
            }
        }
    }
}
