package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
import jakarta.validation.Valid;

import java.util.List;

public interface UserService {

    // ===== 用户端：提交修改申请 =====

    /** 修改资料（昵称/签名） → user_apply (PROFILE) */
    void updateProfile(@Valid UserProfileDTO dto);

    /** 修改头像 → user_apply (AVATAR) */
    void updateAvatar(@Valid CoverUpdateDTO dto);

    // ===== 管理端：审核 =====

    /** 查询审核列表 */
    List<UserApplyVO> getApplyList(Integer status);

    /** 审核通过 */
    void approveApply(Long applyId);

    /** 审核驳回 */
    void rejectApply(Long applyId, String reason);
}
