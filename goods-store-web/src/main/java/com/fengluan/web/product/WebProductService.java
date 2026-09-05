package com.fengluan.web.product;

import com.fengluan.spi.product.dto.GoodCreateRequest;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.dto.GoodUpdateRequest;
import com.fengluan.spi.product.vo.CategoryTreeVO;
import com.fengluan.spi.product.vo.GoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品浏览聚合服务
 */
@Service
@RequiredArgsConstructor
public class WebProductService {

    private final ProductFeignClient productFeignClient;

    public List<GoodVO> list(GoodQueryRequest query) {
        return productFeignClient.page(query);
    }

    public List<CategoryTreeVO> tree() {
        return productFeignClient.categoryTree();
    }

    public GoodVO getById(Long id) {
        return productFeignClient.getById(id);
    }

    public GoodVO create(GoodCreateRequest request) {
        return productFeignClient.create(request);
    }

    public void updateStatus(Long id, Boolean takeDown) {
        productFeignClient.updateStatus(id, takeDown);
    }

    public GoodVO update(Long id, GoodUpdateRequest request) {
        return productFeignClient.update(id, request);
    }

    public void delete(Long id) {
        productFeignClient.delete(id);
    }
}