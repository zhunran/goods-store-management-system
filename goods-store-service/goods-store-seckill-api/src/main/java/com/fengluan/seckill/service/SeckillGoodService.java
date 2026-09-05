package com.fengluan.seckill.service;

import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;

import java.util.List;

public interface SeckillGoodService {
    SeckillGoodVO addGood(Long seckillId, SeckillGoodAddRequest request);
    void removeGood(Long id);
    List<SeckillGoodVO> listByActivity(Long seckillId);
    List<SeckillGoodVO> activeList();
}