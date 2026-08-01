package com.ljc.chaocommunity.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljc.chaocommunity.exception.BusinessException;
import com.ljc.chaocommunity.mapper.AnnouncementMapper;
import com.ljc.chaocommunity.pojo.dto.AnnouncementDTO;
import com.ljc.chaocommunity.pojo.entity.Announcement;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.AdminAnnouncementVO;
import com.ljc.chaocommunity.pojo.vo.AnnouncementVO;
import com.ljc.chaocommunity.service.AnnouncementService;
import com.ljc.chaocommunity.util.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    // ==================== 用户端 ====================

    @Override
    public PageResult<AnnouncementVO> list(int page, int size) {
        Page<Announcement> p = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 0)
                .orderByDesc(Announcement::getIsTop)
                .orderByAsc(Announcement::getSort)
                .orderByDesc(Announcement::getCreateTime);
        Page<Announcement> result = announcementMapper.selectPage(p, wrapper);
        List<AnnouncementVO> voList = new ArrayList<>();
        for (Announcement a : result.getRecords()) {
            AnnouncementVO vo = new AnnouncementVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setType(a.getType());
            vo.setIsTop(a.getIsTop());
            vo.setViewCount(a.getViewCount());
            vo.setCreateTime(a.getCreateTime());
            voList.add(vo);
        }
        return new PageResult<>(result.getTotal(), voList);
    }

    @Override
    @Transactional
    public AnnouncementVO getDetail(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null || a.getStatus() != 0) {
            throw new BusinessException("公告不存在");
        }

        // 浏览量 +1
        Announcement update = new Announcement();
        update.setId(id);
        update.setViewCount(a.getViewCount() + 1);
        announcementMapper.updateById(update);

        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setContent(a.getContent());
        vo.setType(a.getType());
        vo.setIsTop(a.getIsTop());
        vo.setViewCount(a.getViewCount() + 1);
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }

    @Override
    public AnnouncementVO getLatest() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 0)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT 1");
        Announcement a = announcementMapper.selectOne(wrapper);
        if (a == null) {
            return null;
        }
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setType(a.getType());
        vo.setIsTop(a.getIsTop());
        vo.setViewCount(a.getViewCount());
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }

    // ==================== 管理端 ====================

    @Override
    public List<AdminAnnouncementVO> adminList() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getIsTop)
                .orderByAsc(Announcement::getSort)
                .orderByDesc(Announcement::getCreateTime);
        List<Announcement> list = announcementMapper.selectList(wrapper);
        List<AdminAnnouncementVO> voList = new ArrayList<>();
        for (Announcement a : list) {
            AdminAnnouncementVO vo = new AdminAnnouncementVO();
            BeanUtils.copyProperties(a, vo);
            voList.add(vo);
        }
        return voList;
    }

    @Override
    @Transactional
    public void create(AnnouncementDTO dto) {
        Announcement a = new Announcement();
        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        a.setType(dto.getType() != null ? dto.getType() : 1);
        a.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : 0);
        a.setSort(dto.getSort() != null ? dto.getSort() : 0);
        a.setStatus(0);
        a.setCreateUserId(SecurityUtils.getCurrentUserId());
        announcementMapper.insert(a);
    }

    @Override
    @Transactional
    public void update(AnnouncementDTO dto) {
        Announcement a = announcementMapper.selectById(dto.getId());
        if (a == null) {
            throw new BusinessException("公告不存在");
        }

        a.setTitle(dto.getTitle());
        a.setContent(dto.getContent());
        if (dto.getType() != null) {
            a.setType(dto.getType());
        }
        if (dto.getIsTop() != null) {
            a.setIsTop(dto.getIsTop());
        }
        if (dto.getSort() != null) {
            a.setSort(dto.getSort());
        }
        announcementMapper.updateById(a);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("公告不存在");
        }
        a.setStatus(1);
        announcementMapper.updateById(a);
    }

    @Override
    @Transactional
    public void publish(Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException("公告不存在");
        }
        a.setStatus(0);
        announcementMapper.updateById(a);
    }
}
