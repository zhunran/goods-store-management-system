package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.dto.SeckillOrderResponse;
import com.fengluan.spi.seckill.dto.SeckillOrderResultVO;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 秒杀聚合（BFF）：浏览 + 抢购 + 结果轮询 */
@Service
@RequiredArgsConstructor
public class WebSeckillService {

    private final SeckillGoodFeignClient seckillGoodFeignClient;
    private final SeckillOrderFeignClient seckillOrderFeignClient;

    public List<SeckillGoodVO> list() {
        return seckillGoodFeignClient.activeList();
    }

    public SeckillOrderResponse seckill(Long seckillGoodId) {
        return seckillOrderFeignClient.seckill(seckillGoodId);
    }

    public SeckillOrderResultVO result(String orderNo) {
        return seckillOrderFeignClient.result(orderNo);
    }
}