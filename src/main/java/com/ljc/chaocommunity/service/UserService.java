package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CoverUpdateDTO;
import com.ljc.chaocommunity.pojo.dto.UserProfileDTO;
import com.ljc.chaocommunity.pojo.vo.UserApplyVO;
import com.ljc.chaocommunity.pojo.vo.UserVO;
import jakarta.validation.Valid;

import java.util.List;

public interface UserService {

    // ===== 用户端：提交修改申请 =====

    /** 修改资料（昵称/签名） → user_apply (PROFILE) */
    void updateProfile(@Valid UserProfileDTO dto);

    /** 修改头像 → user_apply (AVATAR) */
    void updateAvatar(@Valid CoverUpdateDTO dto);

    // ===== 用户资料查询 =====

    /** 查看自己的完整资料 */
    UserVO getMyProfile();

    /** 查看别人的资料（不含隐私字段） */
    UserVO getUserProfile(Long userId);

    // ===== 管理端：用户管理 =====

    /** 查询所有用户 */
    List<UserVO> listAllUsers();

    /** 根据ID或用户名查询用户详情 */
    UserVO adminGetUserDetail(Long userId);

    /** 根据用户名搜索用户 */
    List<UserVO> searchUsers(String keyword);

    /** 封禁/解封用户 */
    void toggleBanUser(Long userId);

    // ===== 管理端：审核 =====

    /** 查询审核列表 */
    List<UserApplyVO> getApplyList(Integer status);

    /** 审核通过 */
    void approveApply(Long applyId);

    /** 审核驳回 */
    void rejectApply(Long applyId, String reason);
}
