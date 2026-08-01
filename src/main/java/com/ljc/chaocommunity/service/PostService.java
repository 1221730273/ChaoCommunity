package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import jakarta.validation.Valid;

import java.util.List;

public interface PostService {
    /** 创建帖子 → 提交审核，返回审核记录ID */
    Long createPost(@Valid PostDTO dto);

    void deletePost(Long postId);

    /** 更新帖子 → 提交审核，返回审核记录ID */
    Long updatePost(@Valid PostDTO dto);

    /** 更新封面 → 提交审核，返回审核记录ID */
    Long updateCover(Long postId, CoverUpdateDTO dto);

    PostVO getPostVOById(Long postId);

    PageResult<PostVO> pageQuery(PostPageQueryDTO dto);

    /** 根据用户ID查询帖子（查自己=全部状态，查别人=仅可见） */
    PageResult<PostVO> getUserPosts(Long userId, PostPageQueryDTO dto);

    /** 切换帖子隐藏状态（0↔1），仅本人 */
    void toggleHidePost(Long postId);

    /** 查询自己的帖子（包括隐藏的，仅从post表） */
    PageResult<PostVO> getMyPosts(PostPageQueryDTO dto);

    /** 查询自己的审核记录（post_audit中post_id为null的新帖审核） */
    List<PostAuditVO> getMyAudits();

    /** 删除自己审核失败的记录（status=2） */
    void deleteFailedAudit(Long auditId);

    /** 分页查询所有帖子（管理端，包含隐藏） */
    PageResult<PostVO> pageQueryAll(PostPageQueryDTO dto);

    /** 删除帖子（管理端，不限本人） */
    void adminDeletePost(Long postId);

    /** 设置/取消精选 */
    void toggleFeatured(Long postId);

    /** 分页查询精选帖子 */
    PageResult<PostVO> pageQueryFeatured(int page, int size);

    /** 浏览量+1 */
    void incrementViewCount(Long postId);
}
