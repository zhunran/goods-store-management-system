package com.fengluan.web.seckill;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import com.fengluan.spi.seckill.vo.SeckillVO;
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

import java.util.List;

/**
 * 秒杀活动管理聚合入口：/app/api/seckill/admin/activity/**（管理端，需 admin 角色）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/seckill/admin/activity")
public class WebSeckillAdminController {

    private final WebSeckillAdminService webSeckillAdminService;

    @GetMapping("/page")
    public ApiResult<PageVO<SeckillVO>> page(@RequestParam Long pageNum,
                                             @RequestParam Long pageSize,
                                             @RequestParam(required = false) String name) {
        return ApiResult.success(webSeckillAdminService.page(pageNum, pageSize, name));
    }

    @GetMapping("/{id}")
    public ApiResult<SeckillVO> detail(@PathVariable Long id) {
        return ApiResult.success(webSeckillAdminService.detail(id));
    }

    @PostMapping
    public ApiResult<SeckillVO> create(@RequestBody SeckillActivityRequest request) {
        return ApiResult.success(webSeckillAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResult<SeckillVO> update(@PathVariable Long id, @RequestBody SeckillActivityRequest request) {
        return ApiResult.success(webSeckillAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        webSeckillAdminService.delete(id);
        return ApiResult.success();
    }

    @PostMapping("/{id}/goods")
    public ApiResult<SeckillGoodVO> addGood(@PathVariable Long id, @RequestBody SeckillGoodAddRequest request) {
        return ApiResult.success(webSeckillAdminService.addGood(id, request));
    }

    @GetMapping("/{id}/goods")
    public ApiResult<List<SeckillGoodVO>> listGoods(@PathVariable Long id) {
        return ApiResult.success(webSeckillAdminService.listGoods(id));
    }

    @DeleteMapping("/goods/{goodsId}")
    public ApiResult<Void> removeGood(@PathVariable Long goodsId) {
        webSeckillAdminService.removeGood(goodsId);
        return ApiResult.success();
    }
}
