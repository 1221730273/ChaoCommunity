package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.FeedbackMapper;
import com.ljc.chaocommunity.mapper.UserMapper;
import com.ljc.chaocommunity.pojo.dto.FeedbackDTO;
import com.ljc.chaocommunity.pojo.entity.Feedback;
import com.ljc.chaocommunity.pojo.entity.User;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.FeedbackVO;
import com.ljc.chaocommunity.service.FeedbackService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public void submit(FeedbackDTO dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Feedback feedback = new Feedback();
        feedback.setUserId(currentUserId);
        feedback.setType(dto.getType());
        feedback.setContent(dto.getContent());
        feedback.setContact(dto.getContact());
        feedback.setStatus(0);
        feedbackMapper.insert(feedback);
    }

    @Override
    public PageResult<FeedbackVO> list(String type, Integer status, int page, int size) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isBlank()) {
            wrapper.eq(Feedback::getType, type);
        }
        if (status != null) {
            wrapper.eq(Feedback::getStatus, status);
        }
        wrapper.orderByDesc(Feedback::getCreateTime);

        Page<Feedback> p = new Page<>(page, size);
        Page<Feedback> resultPage = feedbackMapper.selectPage(p, wrapper);

        List<FeedbackVO> voList = resultPage.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(resultPage.getTotal(), voList);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        feedbackMapper.deleteById(id);
    }

    private FeedbackVO toVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setType(feedback.getType());
        vo.setContent(feedback.getContent());
        vo.setContact(feedback.getContact());
        vo.setStatus(feedback.getStatus());
        vo.setCreateTime(feedback.getCreateTime());

        User user = userMapper.selectById(feedback.getUserId());
        if (user != null) {
            vo.setUsername(user.getUsername());
        }

        return vo;
    }
}
