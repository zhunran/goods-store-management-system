package com.fengluan.member.service;

import com.fengluan.spi.member.vo.RegionVO;

import java.util.List;

public interface RegionService {

    /** 查询下级行政区划（parentId 为 null/0 时取省级） */
    List<RegionVO> children(Long parentId);
}