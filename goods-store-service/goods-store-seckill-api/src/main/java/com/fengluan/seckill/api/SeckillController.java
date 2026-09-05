package com.fengluan.seckill.api;

import com.fengluan.spi.seckill.SeckillActivityApi;
import com.fengluan.spi.seckill.dto.PageVO;
import com.fengluan.spi.seckill.dto.SeckillActivityRequest;
import com.fengluan.spi.seckill.vo.SeckillVO;
import com.fengluan.seckill.service.SeckillActivityService;
import jakarta.validation.Valid;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/api")
public class SeckillController implements SeckillActivityApi {

    private final SeckillActivityService seckillActivityService;

    @Override
    public SeckillVO create(@Valid @RequestBody SeckillActivityRequest request) {
        return seckillActivityService.create(request);
    }

    @Override
    public SeckillVO update(@PathVariable Long id, @Valid @RequestBody SeckillActivityRequest request) {
        return seckillActivityService.update(id, request);
    }

    @Override
    public Void delete(@PathVariable Long id) {
        seckillActivityService.delete(id);
        return null;
    }

    @Override
    public SeckillVO detail(@PathVariable Long id) {
        return seckillActivityService.detail(id);
    }

    @Override
    public PageVO<SeckillVO> page(@RequestParam Long pageNum,
                                  @RequestParam Long pageSize,
                                  @RequestParam(required = false) String name) {
        return seckillActivityService.page(pageNum, pageSize, name);
    }
}