package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论分页查询")
public class CommentPageQueryDTO {

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "排序方式：hot=最热(默认，按点赞量)  newest=最新")
    private String sort = "hot";
}
