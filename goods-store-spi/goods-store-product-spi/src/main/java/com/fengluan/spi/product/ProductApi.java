package com.fengluan.spi.product;

import com.fengluan.spi.product.dto.GoodCreateRequest;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.dto.GoodUpdateRequest;
import com.fengluan.spi.product.vo.CategoryTreeVO;
import com.fengluan.spi.product.vo.GoodVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 商品服务纯 HTTP 契约（Day 6 起规范：spi 禁 @FeignClient，controller implements、消费方 extends 复用）
 * 相对路径基于实现方 Controller 类级前缀 /good/api（GoodController），完整路径见实现类。
 * 库存扣减/恢复为内部契约方法，供 trade-api/seckill-api extends 复用，web 不暴露。
 */
public interface ProductApi {

    /** 商品分类树 */
    @GetMapping("/category/tree")
    List<CategoryTreeVO> categoryTree();

    /** 商品分页列表（多条件 + 关键词） */
    @GetMapping("/page")
    List<GoodVO> page(GoodQueryRequest request);

    /** 商品详情 */
    @GetMapping("/{id}")
    GoodVO getById(@PathVariable Long id);

    /** 新增商品（含详情图） */
    @PostMapping
    GoodVO create(@Valid @RequestBody GoodCreateRequest request);

    /** 编辑商品 */
    @PutMapping("/{id}")
    GoodVO update(@PathVariable Long id, @Valid @RequestBody GoodUpdateRequest request);

    /** 删除商品（逻辑删除） */
    @DeleteMapping("/{id}")
    Void delete(@PathVariable Long id);

    /** 上下架 */
    @PutMapping("/{id}/status")
    Void updateStatus(@PathVariable Long id, @RequestParam Boolean takeDown);

    /** 扣减库存：true=成功，false=库存不足（原子，防超卖） */
    @PutMapping("/{id}/stock/deduct")
    Boolean deductStock(@PathVariable Long id, @RequestParam Integer count);

    /** 恢复库存 */
    @PutMapping("/{id}/stock/restore")
    Void restoreStock(@PathVariable Long id, @RequestParam Integer count);
}