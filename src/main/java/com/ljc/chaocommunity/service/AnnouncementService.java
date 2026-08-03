package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.AnnouncementDTO;
import com.ljc.chaocommunity.pojo.result.PageResult;
import com.ljc.chaocommunity.pojo.vo.AdminAnnouncementVO;
import com.ljc.chaocommunity.pojo.vo.AnnouncementVO;

import java.util.List;

/**
 * 公告服务
 */
public interface AnnouncementService {

    // ===== 用户端 =====

    /** 分页查询展示中的公告 */
    PageResult<AnnouncementVO> list(int page, int size);

    /** 公告详情（+浏览量） */
    AnnouncementVO getDetail(Long id);

    /** 最新公告（置顶优先，再按时间，limit 条） */
    List<AnnouncementVO> getLatest(int limit);

    // ===== 管理端 =====

    /** 查询所有公告（含下架） */
    List<AdminAnnouncementVO> adminList();

    /** 新增公告 */
    void create(AnnouncementDTO dto);

    /** 修改公告 */
    void update(AnnouncementDTO dto);

    /** 下架公告（status=1） */
    void delete(Long id);

    /** 上架公告（status=0） */
    void publish(Long id);
}
