package com.fengluan.spi.seckill;

import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 秒杀商品纯 HTTP 契约（关联商品 + 用户端有效列表，抢购 Day13 预留）
 * 相对路径基于实现方 Controller 类级前缀 /seckill/api（SeckillGoodController）。
 */
public interface SeckillGoodApi {

    /** 给活动添加秒杀商品 */
    @PostMapping("/activity/{seckillId}/goods")
    SeckillGoodVO addGood(@PathVariable Long seckillId, @Valid @RequestBody SeckillGoodAddRequest request);

    /** 移除活动下的秒杀商品 */
    @DeleteMapping("/activity/goods/{id}")
    Void removeGood(@PathVariable Long id);

    /** 管理端：按活动查询其下全部秒杀商品（不限制活动时间窗口与启用状态） */
    @GetMapping("/activity/{seckillId}/goods")
    List<SeckillGoodVO> listByActivity(@PathVariable Long seckillId);

    /** 用户端：当前有效活动下的秒杀商品列表 */
    @GetMapping("/list")
    List<SeckillGoodVO> activeList();
}