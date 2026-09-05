package com.fengluan.web.seckill;

import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import com.fengluan.spi.seckill.vo.SeckillVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 秒杀活动管理聚合服务（BFF）：活动 CRUD/分页 + 秒杀商品添加/移除
 */
@Service
@RequiredArgsConstructor
public class WebSeckillAdminService {

    private final SeckillActivityFeignClient seckillActivityFeignClient;
    private final SeckillGoodFeignClient seckillGoodFeignClient;

    public SeckillVO create(SeckillActivityRequest request) {
        return seckillActivityFeignClient.create(request);
    }

    public SeckillVO update(Long id, SeckillActivityRequest request) {
        return seckillActivityFeignClient.update(id, request);
    }

    public void delete(Long id) {
        seckillActivityFeignClient.delete(id);
    }

    public SeckillVO detail(Long id) {
        return seckillActivityFeignClient.detail(id);
    }

    public PageVO<SeckillVO> page(Long pageNum, Long pageSize, String name) {
        return seckillActivityFeignClient.page(pageNum, pageSize, name);
    }

    public SeckillGoodVO addGood(Long seckillId, SeckillGoodAddRequest request) {
        return seckillGoodFeignClient.addGood(seckillId, request);
    }

    public List<SeckillGoodVO> listGoods(Long seckillId) {
        return seckillGoodFeignClient.listByActivity(seckillId);
    }

    public void removeGood(Long id) {
        seckillGoodFeignClient.removeGood(id);
    }
}
