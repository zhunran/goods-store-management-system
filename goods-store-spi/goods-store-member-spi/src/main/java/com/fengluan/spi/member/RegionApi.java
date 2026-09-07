package com.fengluan.spi.member;

import com.fengluan.spi.member.vo.RegionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 行政区划纯 HTTP 契约。
 * 相对路径基于实现方 Controller 类级前缀 /region/api。
 * 首次调用 parentId=0（或省略）取省级；后续用上一级 id 取下级，只查有效行。
 */
public interface RegionApi {

    /** 查询下级行政区划（parentId=0 取省级） */
    @GetMapping("/children")
    List<RegionVO> children(@RequestParam(value = "parentId", defaultValue = "0") Long parentId);
}