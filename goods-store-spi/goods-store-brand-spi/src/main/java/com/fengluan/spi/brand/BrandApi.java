package com.fengluan.spi.brand;

import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 品牌服务纯 HTTP 契约（Day 5 起规范：spi 禁 @FeignClient，controller implements、消费方 extends 复用）
 * 上传 upload 涉及 MultipartFile，属人机交互，不进契约。
 */
public interface BrandApi {

    @GetMapping("/page")
    PageVO<BrandVO> page(BrandQueryRequest query);

    @GetMapping("/{id}")
    BrandVO getById(@PathVariable Long id);

    @PostMapping
    BrandVO create(@Valid @RequestBody BrandCreateRequest request);

    @PutMapping("/{id}")
    BrandVO update(@PathVariable Long id, @Valid @RequestBody BrandUpdateRequest request);

    @DeleteMapping("/{id}")
    Void delete(@PathVariable Long id);
}