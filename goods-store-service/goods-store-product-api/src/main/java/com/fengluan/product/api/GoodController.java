package com.fengluan.product.api;

import com.fengluan.product.service.CategoryService;
import com.fengluan.product.service.GoodService;
import com.fengluan.spi.product.ProductApi;
import com.fengluan.spi.product.dto.GoodCreateRequest;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.dto.GoodUpdateRequest;
import com.fengluan.spi.product.vo.CategoryTreeVO;
import com.fengluan.spi.product.vo.GoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/good/api")
@RequiredArgsConstructor
public class GoodController implements ProductApi {

    private final GoodService goodService;
    private final CategoryService categoryService;

    @Override
    public List<CategoryTreeVO> categoryTree() {
        return categoryService.getTree();
    }

    @Override
    public List<GoodVO> page(GoodQueryRequest request) {
        return goodService.page(request);
    }

    @Override
    public GoodVO getById(Long id) {
        return goodService.detail(id);
    }

    @Override
    public GoodVO create(GoodCreateRequest request) {
        return goodService.create(request);
    }

    @Override
    public GoodVO update(Long id, GoodUpdateRequest request) {
        return goodService.update(id, request);
    }

    @Override
    public Void delete(Long id) {
        goodService.delete(id);
        return null;
    }

    @Override
    public Void updateStatus(Long id, Boolean takeDown) {
        goodService.updateStatus(id, takeDown);
        return null;
    }

    @Override
    public Boolean deductStock(Long id, Integer count) {
        return goodService.deductStock(id, count);
    }

    @Override
    public Void restoreStock(Long id, Integer count) {
        goodService.restoreStock(id, count);
        return null;
    }
}