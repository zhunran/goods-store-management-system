package com.fengluan.seckill.api;

import com.fengluan.spi.seckill.SeckillGoodApi;
import com.fengluan.spi.seckill.dto.SeckillGoodAddRequest;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import com.fengluan.seckill.service.SeckillGoodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/seckill/api")
public class SeckillGoodController implements SeckillGoodApi {

    private final SeckillGoodService seckillGoodService;

    @Override
    public SeckillGoodVO addGood(@PathVariable Long seckillId,
                                 @Valid @RequestBody SeckillGoodAddRequest request) {
        return seckillGoodService.addGood(seckillId, request);
    }

    @Override
    public Void removeGood(@PathVariable Long id) {
        seckillGoodService.removeGood(id);
        return null;
    }

    @Override
    public List<SeckillGoodVO> listByActivity(@PathVariable Long seckillId) {
        return seckillGoodService.listByActivity(seckillId);
    }

    @Override
    public List<SeckillGoodVO> activeList() {
        return seckillGoodService.activeList();
    }
}