package com.fengluan.web.product;

import com.fengluan.spi.product.ProductApi;
import com.fengluan.spi.product.dto.GoodQueryRequest;
import com.fengluan.spi.product.vo.GoodVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * web 层商品 Feign 客户端：extends product 纯契约
 */
@FeignClient(name = "goods-store-product-api", path = "/good/api")
public interface ProductFeignClient extends ProductApi {

    /**
     * GET 请求的 POJO 参数必须用 @SpringQueryMap 展开为 query 参数。
     */
    @GetMapping("/page")
    List<GoodVO> page(@SpringQueryMap GoodQueryRequest request);
}
