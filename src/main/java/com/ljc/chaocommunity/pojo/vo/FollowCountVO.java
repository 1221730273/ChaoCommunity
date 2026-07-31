package com.ljc.chaocommunity.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注/粉丝数量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关注统计")
public class FollowCountVO {

    @Schema(description = "关注数（TA关注了多少人）")
    private Integer followCount;

    @Schema(description = "粉丝数（多少人关注了TA）")
    private Integer followerCount;
}
