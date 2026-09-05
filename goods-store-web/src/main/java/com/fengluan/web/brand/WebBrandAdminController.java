package com.fengluan.web.brand;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 品牌管理聚合入口：/app/api/brand/admin/**（管理端，需 admin 角色）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/brand/admin")
public class WebBrandAdminController {

    private final WebBrandService webBrandService;

    @GetMapping("/page")
    public ApiResult<PageVO<BrandVO>> page(BrandQueryRequest query) {
        return ApiResult.success(webBrandService.page(query));
    }

    @PostMapping
    public ApiResult<BrandVO> create(@RequestBody BrandCreateRequest request) {
        return ApiResult.success(webBrandService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<BrandVO> update(@PathVariable Long id, @RequestBody BrandUpdateRequest request) {
        return ApiResult.success(webBrandService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        webBrandService.delete(id);
        return ApiResult.success();
    }
}
