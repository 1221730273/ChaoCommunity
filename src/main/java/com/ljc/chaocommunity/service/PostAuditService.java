package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.PostAuditVO;

import java.util.List;

/**
 * 帖子审核服务
 */
public interface PostAuditService {

    /**
     * 分页查询审核列表
     * @param status 审核状态（null=全部，0=待审核，1=通过，2=拒绝）
     */
    PageResult<PostAuditVO> getAuditList(Integer status, int page, int size);

    /**
     * 审核通过
     */
    void approveAudit(Long auditId);

    /**
     * 审核拒绝
     */
    void rejectAudit(Long auditId, String reason);

    /**
     * 根据用户ID查询审核记录
     */
    List<PostAuditVO> getAuditListByUserId(Long userId);

    /**
     * 删除审核记录（管理端）
     */
    void deleteAudit(Long auditId);
}
