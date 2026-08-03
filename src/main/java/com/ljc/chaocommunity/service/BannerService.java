package com.ljc.chaocommunity.service;

import com.ljc.chaocommunity.pojo.dto.CreateBannerDTO;
import com.ljc.chaocommunity.pojo.dto.UpdateBannerDTO;
import com.ljc.chaocommunity.pojo.vo.AdminBannerVO;
import com.ljc.chaocommunity.pojo.vo.BannerVO;

import java.util.List;

public interface BannerService {

    /** 用户端：查询展示中的轮播图 */
    List<BannerVO> getUserBanners();

    /** 管理端：查询所有轮播图（含关闭的） */
    List<AdminBannerVO> getAdminBanners();

    /** 新增轮播图 */
    void create(CreateBannerDTO dto);

    /** 更新轮播图 */
    void update(UpdateBannerDTO dto);

    /** 删除轮播图（逻辑删除 + 释放文件） */
    void delete(Long id);
}
