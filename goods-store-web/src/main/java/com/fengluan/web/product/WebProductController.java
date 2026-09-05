package com.fengluan.web.product;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.vo.CategoryTreeVO;
import com.fengluan.spi.product.vo.GoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * web 层商品浏览聚合入口：前端唯一入口 /app/api/**
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/product")
public class WebProductController {

    private final WebProductService webProductService;

    @GetMapping("/list")
    public ApiResult<List<GoodVO>> list(GoodQueryRequest query) {
        return ApiResult.success(webProductService.list(query));
    }

    @GetMapping("/tree")
    public ApiResult<List<CategoryTreeVO>> tree() {
        return ApiResult.success(webProductService.tree());
    }

    /** 商品详情（C 端，免鉴权） */
    @GetMapping("/{id}")
    public ApiResult<GoodVO> getById(@PathVariable Long id) {
        return ApiResult.success(webProductService.getById(id));
    }
}