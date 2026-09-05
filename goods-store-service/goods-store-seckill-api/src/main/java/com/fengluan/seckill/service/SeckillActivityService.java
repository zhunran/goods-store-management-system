package com.fengluan.seckill.service;

import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import com.fengluan.seckill.entity.SeckillEntity;

public interface SeckillActivityService {
    SeckillVO create(SeckillActivityRequest request);
    SeckillVO update(Long id, SeckillActivityRequest request);
    void delete(Long id);
    SeckillVO detail(Long id);
    PageVO<SeckillVO> page(Long pageNum, Long pageSize, String name);
    SeckillEntity getById(Long id); // 校验活动存在，供 SeckillGoodService 用
}