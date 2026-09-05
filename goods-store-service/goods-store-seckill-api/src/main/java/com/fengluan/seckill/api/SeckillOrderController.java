package com.fengluan.seckill.api;

import com.fengluan.seckill.service.SeckillOrderService;
import com.fengluan.seckill.util.CurrentUserUtil;
import com.fengluan.spi.seckill.SeckillOrderApi;
import com.fengluan.spi.seckill.dto.SeckillOrderResponse;
import com.fengluan.spi.seckill.dto.SeckillOrderResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 秒杀抢购/结果轮询实现（controller implements 契约） */
@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/api")
public class SeckillOrderController implements SeckillOrderApi {

    private final SeckillOrderService seckillOrderService;

    @Override
    public SeckillOrderResponse seckill(@PathVariable Long seckillGoodId) {
        return seckillOrderService.seckill(seckillGoodId, CurrentUserUtil.currentUserId());
    }

    @Override
    public SeckillOrderResultVO result(@PathVariable String orderNo) {
        return seckillOrderService.result(orderNo, CurrentUserUtil.currentUserId());
    }
}