package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.CategoryMapper;
import com.ljc.chaocommunity.mapper.FileRecordMapper;
import com.ljc.chaocommunity.mapper.PostFileMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.mapper.PostTagMapper;
import com.ljc.chaocommunity.mapper.TagMapper;
import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.entity.Category;
import com.ljc.chaocommunity.pojo.entity.FileRecord;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.entity.PostFile;
import com.ljc.chaocommunity.pojo.entity.PostTag;
import com.ljc.chaocommunity.pojo.entity.Tag;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import com.ljc.chaocommunity.pojo.vo.TagVO;
import com.ljc.chaocommunity.service.PostService;
import com.ljc.chaocommunity.util.OssUtil;
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
    private OssUtil ossUtil;

    @Autowired
    private FileRecordMapper fileRecordMapper;

    @Autowired
    private PostFileMapper postFileMapper;

    
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

        // 3. 创建帖子对象
        Post post = new Post();
        post.setUserId(SecurityUtils.getCurrentUserId());
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


        // 5. 处理封面文件：移动 temp/ → post/cover/
        if (dto.getFileId() != null) {
            FileRecord fileRecord = fileRecordMapper.selectById(dto.getFileId());
            if (fileRecord == null) {
                throw new BusinessException("文件不存在");
            }
            if(!fileRecord.getUserId()
                    .equals(SecurityUtils.getCurrentUserId())){
                throw new BusinessException("无权使用该文件");
            }
            if(fileRecord.getStatus()==1){
                throw new BusinessException("文件已经被使用");
            }
            if(!fileRecord.getFilePath().startsWith("temp/")){
                throw new BusinessException("非法文件");
            }

            String oldObjectKey = fileRecord.getFilePath();
            String newObjectKey = oldObjectKey.replace("temp/", "post/cover/");
            OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

            fileRecord.setFilePath(moveResult.objectKey());
            fileRecord.setUrl(moveResult.url());
            fileRecord.setStatus(1);
            fileRecordMapper.updateById(fileRecord);

            // 写入帖子-文件关联表
            PostFile postFile = new PostFile();
            postFile.setPostId(post.getId());
            postFile.setFileId(fileRecord.getId());
            postFile.setType("COVER");
            postFileMapper.insert(postFile);

            post.setCoverUrl(moveResult.url());
        }

        // 6. 处理正文图片：移动 temp/ → post/content/，并替换 Markdown 中的临时URL
        String content = post.getContent();
        if (dto.getContentFileIds() != null && !dto.getContentFileIds().isEmpty()) {
            for (Long contentFileId : dto.getContentFileIds()) {
                FileRecord fileRecord = fileRecordMapper.selectById(contentFileId);
                if (fileRecord == null) {
                    throw new BusinessException("正文图片文件不存在，fileId=" + contentFileId);
                }
                if (!fileRecord.getUserId().equals(SecurityUtils.getCurrentUserId())) {
                    throw new BusinessException("无权使用该文件");
                }
                if (fileRecord.getStatus() == 1) {
                    throw new BusinessException("文件已经被使用");
                }
                if (!fileRecord.getFilePath().startsWith("temp/")) {
                    throw new BusinessException("非法文件");
                }

                String oldUrl = fileRecord.getUrl();
                String oldObjectKey = fileRecord.getFilePath();
                String newObjectKey = oldObjectKey.replace("temp/", "post/content/");
                OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

                // 替换 content 中的临时URL为正式URL
                content = content.replace(oldUrl, moveResult.url());

                fileRecord.setFilePath(moveResult.objectKey());
                fileRecord.setUrl(moveResult.url());
                fileRecord.setStatus(1);
                fileRecordMapper.updateById(fileRecord);

                // 写入帖子-文件关联表
                PostFile postFile = new PostFile();
                postFile.setPostId(post.getId());
                postFile.setFileId(fileRecord.getId());
                postFile.setType("CONTENT");
                postFileMapper.insert(postFile);
            }
            post.setContent(content);
        }

        // 7. 更新帖子（coverUrl + content）
        postMapper.updateById(post);

        return post.getId();
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

    @Override
    @Transactional
    public void updatePost(PostDTO dto) {
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

        // 4. 更新帖子基本信息
        post.setTitle(dto.getTitle());
        if (dto.getCategoryId() != null) {
            post.setCategoryId(dto.getCategoryId());
        }

        // 5. 更新标签关联（先删后插）
        if (dto.getTagIds() != null) {
            LambdaQueryWrapper<PostTag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PostTag::getPostId, post.getId());
            postTagMapper.delete(wrapper);

            if (!dto.getTagIds().isEmpty()) {
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
        }

        // 6. 处理正文图片增删
        String content = dto.getContent();

        // 6a. 查询旧的正文图片
        LambdaQueryWrapper<PostFile> postFileWrapper = new LambdaQueryWrapper<>();
        postFileWrapper.eq(PostFile::getPostId, post.getId())
                .eq(PostFile::getType, "CONTENT");
        List<PostFile> oldPostFiles = postFileMapper.selectList(postFileWrapper);
        List<Long> oldFileIds = oldPostFiles.stream()
                .map(PostFile::getFileId)
                .collect(Collectors.toList());

        // 6b. 前端传的新集合
        List<Long> newFileIds = dto.getContentFileIds() != null
                ? dto.getContentFileIds()
                : new ArrayList<>();

        // 6c. 被删除的：old - new
        List<Long> toDelete = oldFileIds.stream()
                .filter(id -> !newFileIds.contains(id))
                .collect(Collectors.toList());

        // 6d. 新增的：new - old
        List<Long> toAdd = newFileIds.stream()
                .filter(id -> !oldFileIds.contains(id))
                .collect(Collectors.toList());

        // 6e. 处理删除：file_record.status=0 + 删 post_file 记录
        for (Long fileId : toDelete) {
            FileRecord fileRecord = fileRecordMapper.selectById(fileId);
            if (fileRecord != null && fileRecord.getStatus() == 1) {
                fileRecord.setStatus(0);
                fileRecordMapper.updateById(fileRecord);
            }
            LambdaQueryWrapper<PostFile> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(PostFile::getPostId, post.getId())
                    .eq(PostFile::getFileId, fileId);
            postFileMapper.delete(deleteWrapper);
        }

        // 6f. 处理新增：校验 → move → 更新 file_record → 插 post_file → 替换 URL
        for (Long fileId : toAdd) {
            FileRecord fileRecord = fileRecordMapper.selectById(fileId);
            if (fileRecord == null) {
                throw new BusinessException("正文图片文件不存在，fileId=" + fileId);
            }
            if (!fileRecord.getUserId().equals(currentUserId)) {
                throw new BusinessException("无权使用该文件");
            }
            if (fileRecord.getStatus() == 1) {
                throw new BusinessException("文件已经被使用");
            }
            if (!fileRecord.getFilePath().startsWith("temp/")) {
                throw new BusinessException("非法文件");
            }

            String oldUrl = fileRecord.getUrl();
            String oldObjectKey = fileRecord.getFilePath();
            String newObjectKey = oldObjectKey.replace("temp/", "post/content/");
            OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

            // 替换 content 中的临时URL为正式URL
            content = content.replace(oldUrl, moveResult.url());

            fileRecord.setFilePath(moveResult.objectKey());
            fileRecord.setUrl(moveResult.url());
            fileRecord.setStatus(1);
            fileRecordMapper.updateById(fileRecord);

            PostFile postFile = new PostFile();
            postFile.setPostId(post.getId());
            postFile.setFileId(fileRecord.getId());
            postFile.setType("CONTENT");
            postFileMapper.insert(postFile);
        }

        // 7. 更新帖子内容
        post.setContent(content);
        postMapper.updateById(post);
    }

    @Override
    @Transactional
    public void updateCover(Long postId, CoverUpdateDTO dto) {
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

        // 2. 查询新文件
        FileRecord newFileRecord = fileRecordMapper.selectById(dto.getFileId());
        if (newFileRecord == null) {
            throw new BusinessException("文件不存在");
        }
        if (!newFileRecord.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权使用该文件");
        }
        if (newFileRecord.getStatus() == 1) {
            throw new BusinessException("文件已经被使用");
        }
        if (!newFileRecord.getFilePath().startsWith("temp/")) {
            throw new BusinessException("非法文件");
        }

        // 3. 通过 post_file 表查询旧封面
        PostFile oldPostFile = null;
        LambdaQueryWrapper<PostFile> postFileWrapper = new LambdaQueryWrapper<>();
        postFileWrapper.eq(PostFile::getPostId, postId)
                .eq(PostFile::getType, "COVER");
        oldPostFile = postFileMapper.selectOne(postFileWrapper);

        // 4. 新旧 file_record.id 一致则直接返回
        if (oldPostFile != null && oldPostFile.getFileId().equals(dto.getFileId())) {
            return;
        }

        // 5. 移动新文件到正式目录
        String oldObjectKey = newFileRecord.getFilePath();
        String newObjectKey = oldObjectKey.replace("temp/", "post/cover/");
        OssUtil.UploadResult moveResult = ossUtil.move(oldObjectKey, newObjectKey);

        // 6. 旧封面：file_record.status 置 0，删除旧的 post_file 记录
        if (oldPostFile != null) {
            FileRecord oldFileRecord = fileRecordMapper.selectById(oldPostFile.getFileId());
            if (oldFileRecord != null) {
                oldFileRecord.setStatus(0);
                fileRecordMapper.updateById(oldFileRecord);
            }
            postFileMapper.deleteById(oldPostFile.getId());
        }

        // 7. 新封面：file_record.status 置 1，插入新的 post_file 记录
        newFileRecord.setFilePath(moveResult.objectKey());
        newFileRecord.setUrl(moveResult.url());
        newFileRecord.setStatus(1);
        fileRecordMapper.updateById(newFileRecord);

        PostFile newPostFile = new PostFile();
        newPostFile.setPostId(postId);
        newPostFile.setFileId(newFileRecord.getId());
        newPostFile.setType("COVER");
        postFileMapper.insert(newPostFile);

        // 8. 更新帖子coverUrl
        post.setCoverUrl(moveResult.url());
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

        // 查询正文图片的 fileRecord ID 列表（用于前端编辑时回传）
        LambdaQueryWrapper<PostFile> postFileWrapper = new LambdaQueryWrapper<>();
        postFileWrapper.eq(PostFile::getPostId, postId)
                .eq(PostFile::getType, "CONTENT");
        List<PostFile> contentFiles = postFileMapper.selectList(postFileWrapper);
        List<Long> contentFileIds = contentFiles.stream()
                .map(PostFile::getFileId)
                .collect(Collectors.toList());
        vo.setContentFileIds(contentFileIds);

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
        Long currentUserId = "follow".equals(dto.getSort()) ? SecurityUtils.getCurrentUserId() : null;

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
}
