package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.HandleReportDTO;
import com.ljc.chaocommunity.pojo.dto.ReportDTO;
import com.ljc.chaocommunity.pojo.vo.ReportVO;

import java.util.List;

public interface ReportService {

    /** 用户提交举报 */
    void submitReport(ReportDTO dto);

    /** 管理端：查询举报列表 */
    List<ReportVO> getReportList(Integer status);

    /** 管理端：处理举报 */
    void handleReport(HandleReportDTO dto);
}
