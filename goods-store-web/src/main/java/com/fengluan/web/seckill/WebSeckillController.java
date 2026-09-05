package com.fengluan.web.seckill;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.seckill.dto.SeckillOrderResponse;
import com.fengluan.spi.seckill.dto.SeckillOrderResultVO;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 秒杀聚合入口：/app/api/seckill/list | order | order/{orderNo}/result */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/seckill")
public class WebSeckillController {

    private final WebSeckillService webSeckillService;

    @GetMapping("/list")
    public ApiResult<List<SeckillGoodVO>> list() {
        return ApiResult.success(webSeckillService.list());
    }

    @PostMapping("/order")
    public ApiResult<SeckillOrderResponse> order(@RequestParam Long seckillGoodId) {
        return ApiResult.success(webSeckillService.seckill(seckillGoodId));
    }

    @GetMapping("/order/{orderNo}/result")
    public ApiResult<SeckillOrderResultVO> result(@PathVariable String orderNo) {
        return ApiResult.success(webSeckillService.result(orderNo));
    }
}