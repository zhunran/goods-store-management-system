package com.fengluan.member.api;

import com.fengluan.member.service.RegionService;
import com.fengluan.spi.member.RegionApi;
import com.fengluan.spi.member.vo.RegionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行政区划服务实现：implements RegionApi 纯契约，与网关 /region/api/** 路由一致。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/region/api")
public class RegionController implements RegionApi {

    private final RegionService regionService;

    @Override
    public List<RegionVO> children(Long parentId) {
        return regionService.children(parentId);
    }
}