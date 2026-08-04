package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.HandleReportDTO;
import com.ljc.chaocommunity.pojo.dto.ReportDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.ReportVO;

public interface ReportService {

    /** 用户提交举报 */
    void submitReport(ReportDTO dto);

    /** 管理端：分页查询举报列表 */
    PageResult<ReportVO> getReportList(Integer status, int page, int size);

    /** 管理端：处理举报 */
    void handleReport(HandleReportDTO dto);

    /** 管理端：删除举报记录 */
    void deleteReport(Long id);
}
