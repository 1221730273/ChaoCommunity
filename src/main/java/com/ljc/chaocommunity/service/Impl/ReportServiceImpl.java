package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.CommentMapper;
import com.ljc.chaocommunity.mapper.PostMapper;
import com.ljc.chaocommunity.mapper.ReportMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.dto.HandleReportDTO;
import com.ljc.chaocommunity.pojo.dto.ReportDTO;
import com.ljc.chaocommunity.pojo.entity.Report;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.vo.ReportVO;
import com.ljc.chaocommunity.service.ReportService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void submitReport(ReportDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 1. 校验目标存在
        switch (dto.getTargetType()) {
            case "POST":
                if (postMapper.selectById(dto.getTargetId()) == null) {
                    throw new BusinessException("帖子不存在");
                }
                break;
            case "COMMENT":
                if (commentMapper.selectById(dto.getTargetId()) == null) {
                    throw new BusinessException("评论不存在");
                }
                break;
            case "USER":
                if (userMapper.selectById(dto.getTargetId()) == null) {
                    throw new BusinessException("用户不存在");
                }
                break;
            default:
                throw new BusinessException("不支持的举报类型");
        }

        // 2. 检查是否已有待处理的举报（防重复）
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getUserId, currentUserId)
                .eq(Report::getTargetId, dto.getTargetId())
                .eq(Report::getTargetType, dto.getTargetType())
                .eq(Report::getStatus, 0);
        if (reportMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("你已经举报过了");
        }

        // 3. 保存举报
        Report report = new Report();
        report.setUserId(currentUserId);
        report.setTargetId(dto.getTargetId());
        report.setTargetType(dto.getTargetType());
        report.setReason(dto.getReason());
        report.setStatus(0);
        reportMapper.insert(report);
    }

    @Override
    public List<ReportVO> getReportList(Integer status) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByDesc(Report::getCreateTime);
        List<Report> reports = reportMapper.selectList(wrapper);

        List<ReportVO> voList = new ArrayList<>();
        for (Report report : reports) {
            ReportVO vo = new ReportVO();
            vo.setId(report.getId());
            vo.setUserId(report.getUserId());
            vo.setTargetId(report.getTargetId());
            vo.setTargetType(report.getTargetType());
            vo.setReason(report.getReason());
            vo.setStatus(report.getStatus());
            vo.setHandlerId(report.getHandlerId());
            vo.setHandleRemark(report.getHandleRemark());
            vo.setCreateTime(report.getCreateTime());

            // 查询举报人用户名
            User user = userMapper.selectById(report.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
            }

            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional
    public void handleReport(HandleReportDTO dto) {
        Report report = reportMapper.selectById(dto.getId());
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException("该举报已处理过");
        }
        if (dto.getStatus() != 1 && dto.getStatus() != 2) {
            throw new BusinessException("无效的处理状态");
        }

        Long handlerId = SecurityUtils.getCurrentUserId();
        report.setStatus(dto.getStatus());
        report.setHandlerId(handlerId);
        report.setHandleRemark(dto.getHandleRemark());
        reportMapper.updateById(report);
    }
}
