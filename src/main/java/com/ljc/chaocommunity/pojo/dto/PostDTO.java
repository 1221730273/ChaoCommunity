package com.ljc.chaocommunity.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "帖子请求（创建/修改共用）")
public class PostDTO {

    @Schema(description = "帖子ID（修改时必传）")
    private Long id;

    @NotNull(message = "分类不能为空")
    @Schema(description = "所属分类ID（创建时必传）")
    private Long categoryId;

    @Size(max = 3, message = "标签最多3个")
    @Schema(description = "标签ID列表（最多3个）")
    private List<Long> tagIds;

    @NotBlank(message = "标题不能为空")
    @Size(max = 15, message = "标题最多15个字")
    @Schema(description = "帖子标题（最多15字）")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "帖子内容")
    private String content;

    @Schema(description = "封面文件ID（创建帖子时使用）")
    private Long fileId;

    @Schema(description = "正文图片文件ID列表（创建帖子时使用）")
    private List<Long> contentFileIds;

}
