package com.fengluan.web.brand;

import com.fengluan.spi.brand.dto.BrandCreateRequest;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.dto.BrandUpdateRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 品牌聚合服务：首页取前 8 条品牌 + 管理端 CRUD
 */
@Service
@RequiredArgsConstructor
public class WebBrandService {

    private final BrandFeignClient brandFeignClient;

    public List<BrandVO> listForHome() {
        BrandQueryRequest q = new BrandQueryRequest();
        q.setPageNum(1L);
        q.setPageSize(8L);
        return brandFeignClient.page(q).getRecords();
    }

    public PageVO<BrandVO> page(BrandQueryRequest query) {
        return brandFeignClient.page(query);
    }

    public BrandVO create(BrandCreateRequest request) {
        return brandFeignClient.create(request);
    }

    public BrandVO update(Long id, BrandUpdateRequest request) {
        request.setId(id);
        return brandFeignClient.update(id, request);
    }

    public void delete(Long id) {
        brandFeignClient.delete(id);
    }
}