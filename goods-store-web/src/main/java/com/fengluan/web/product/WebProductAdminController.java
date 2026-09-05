package com.fengluan.web.product;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.product.dto.GoodCreateRequest;
import com.fengluan.spi.product.dto.GoodUpdateRequest;
import com.fengluan.spi.product.vo.GoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * web 层商品管理聚合入口：管理端（需登录鉴权，未加白名单）
 * 库存扣减/恢复为内部契约方法（trade/seckill 消费），web 不暴露。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/product/admin")
public class WebProductAdminController {

    private final WebProductService webProductService;

    @GetMapping("/{id}")
    public ApiResult<GoodVO> getById(@PathVariable Long id) {
        return ApiResult.success(webProductService.getById(id));
    }

    @PostMapping
    public ApiResult<GoodVO> create(@RequestBody GoodCreateRequest request) {
        return ApiResult.success(webProductService.create(request));
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestParam Boolean takeDown) {
        webProductService.updateStatus(id, takeDown);
        return ApiResult.success();
    }

    @PutMapping("/{id}")
    public ApiResult<GoodVO> update(@PathVariable Long id, @RequestBody GoodUpdateRequest request) {
        return ApiResult.success(webProductService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        webProductService.delete(id);
        return ApiResult.success();
    }
}