package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT p.id, p.user_id, p.category_id, p.title, p.content, " +
            "p.view_count, p.like_count, p.comment_count, p.cover_url, p.top, p.status, p.is_featured, " +
            "p.create_time, p.update_time, " +
            "u.username, u.nickname, u.avatar, " +
            "c.name AS category_name " +
            "FROM post p " +
            "LEFT JOIN user u ON p.user_id = u.id " +
            "LEFT JOIN category c ON p.category_id = c.id " +
            "WHERE p.id = #{postId}")
    PostVO getPostVOById(Long postId);

    /** 首页：按分类 + new/hot/follow */
    Page<PostVO> selectPageVo(Page<PostVO> page,
                              @Param("categoryId") Long categoryId,
                              @Param("sort") String sort,
                              @Param("currentUserId") Long currentUserId);

    /** 用户主页：按用户ID + new/hot，includeAllStatus=true时不过滤status */
    Page<PostVO> selectPageVoByUserId(Page<PostVO> page,
                                      @Param("userId") Long userId,
                                      @Param("sort") String sort,
                                      @Param("includeAllStatus") Boolean includeAllStatus);

    /** 分页查询所有帖子（管理端，包含隐藏） */
    Page<PostVO> selectPageVoAll(Page<PostVO> page,
                                 @Param("categoryId") Long categoryId,
                                 @Param("sort") String sort);

    /** 分页查询精选帖子 */
    Page<PostVO> selectPageVoFeatured(Page<PostVO> page);

}
