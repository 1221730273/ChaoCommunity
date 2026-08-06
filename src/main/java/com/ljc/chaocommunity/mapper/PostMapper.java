package com.ljc.chaocommunity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.pojo.entity.Post;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    // ===== 首页 =====

    Page<PostVO> selectPageVoNewest(Page<PostVO> page,
                                    @Param("categoryId") Long categoryId);

    Page<PostVO> selectPageVoHot(Page<PostVO> page,
                                 @Param("categoryId") Long categoryId);

    Page<PostVO> selectPageVoFollow(Page<PostVO> page,
                                    @Param("categoryId") Long categoryId,
                                    @Param("currentUserId") Long currentUserId);

    // ===== 用户主页 =====

    Page<PostVO> selectPageVoByUserIdNewest(Page<PostVO> page,
                                            @Param("userId") Long userId,
                                            @Param("includeAllStatus") Boolean includeAllStatus);

    Page<PostVO> selectPageVoByUserIdHot(Page<PostVO> page,
                                          @Param("userId") Long userId,
                                          @Param("includeAllStatus") Boolean includeAllStatus);

    // ===== 管理端 =====

    Page<PostVO> selectPageVoAllNewest(Page<PostVO> page,
                                       @Param("categoryId") Long categoryId);

    Page<PostVO> selectPageVoAllHot(Page<PostVO> page,
                                    @Param("categoryId") Long categoryId);

    // ===== 精选 =====

    Page<PostVO> selectPageVoFeaturedNewest(Page<PostVO> page);

    Page<PostVO> selectPageVoFeaturedHot(Page<PostVO> page);

    /** 批量根据ID查询PostVO，结果顺序不定，调用方需自行重排序 */
    List<PostVO> getPostVOsByIds(@Param("ids") List<Long> ids);

}
