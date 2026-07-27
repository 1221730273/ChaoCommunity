package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "帖子分页查询")
public class PostPageQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "分类ID（不传则查全部）")
    private Long categoryId;

    @Schema(description = "排序方式：newest=最新(默认)  hot=最热", example = "newest")
    private String sort = "newest";

}
