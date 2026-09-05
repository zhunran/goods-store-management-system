package com.fengluan.spi.seckill;

import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 秒杀活动管理纯 HTTP 契约
 * 相对路径基于实现方 Controller 类级前缀 /seckill/api（SeckillController）。
 */
public interface SeckillActivityApi {

    /** 创建秒杀活动 */
    @PostMapping("/activity")
    SeckillVO create(@Valid @RequestBody SeckillActivityRequest request);

    /** 更新秒杀活动 */
    @PutMapping("/activity/{id}")
    SeckillVO update(@PathVariable Long id, @Valid @RequestBody SeckillActivityRequest request);

    /** 删除秒杀活动 */
    @DeleteMapping("/activity/{id}")
    Void delete(@PathVariable Long id);

    /** 秒杀活动详情 */
    @GetMapping("/activity/{id}")
    SeckillVO detail(@PathVariable Long id);

    /** 秒杀活动分页 */
    @GetMapping("/activity/page")
    PageVO<SeckillVO> page(@RequestParam Long pageNum,
                           @RequestParam Long pageSize,
                           @RequestParam(required = false) String name);
}