package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.PostDTO;
import com.ljc.chaocommunity.pojo.dto.PostPageQueryDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostVO;
import jakarta.validation.Valid;

public interface PostService {
    Long createPost(@Valid PostDTO dto);

    void deletePost(Long postId);

    void updatePost(@Valid PostDTO dto);

    void updateCover(Long postId, CoverUpdateDTO dto);

    PostVO getPostVOById(Long postId);

    PageResult<PostVO> pageQuery(PostPageQueryDTO dto);

    PageResult<PostVO> getUserPosts(Long userId, PostPageQueryDTO dto);
}
