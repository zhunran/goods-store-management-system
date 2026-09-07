package com.fengluan.web.region;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.member.vo.RegionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * web 层行政区划聚合入口：GET /app/api/region/children，C 端登录态即可（不进白名单）。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/region")
public class WebRegionController {

    private final RegionFeignClient regionFeignClient;

    @GetMapping("/children")
    public ApiResult<List<RegionVO>> children(
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId) {
        return ApiResult.success(regionFeignClient.children(parentId));
    }
}