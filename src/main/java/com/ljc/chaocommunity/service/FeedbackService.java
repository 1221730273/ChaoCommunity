package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.FeedbackDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.FeedbackVO;

/**
 * 反馈服务
 */
public interface FeedbackService {

    /** 用户提交反馈 */
    void submit(FeedbackDTO dto);

    /** 管理端：分页查询反馈列表 */
    PageResult<FeedbackVO> list(String type, Integer status, int page, int size);

    /** 管理端：删除反馈记录 */
    void delete(Long id);
}
