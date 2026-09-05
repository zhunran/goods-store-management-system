package com.fengluan.web.brand;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.brand.vo.BrandVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * web 层品牌聚合入口：前端唯一入口 /app/api/**
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/brand")
public class WebBrandController {

    private final WebBrandService webBrandService;

    @GetMapping("/list")
    public ApiResult<List<BrandVO>> listForHome() {
        return ApiResult.success(webBrandService.listForHome());
    }
}