package com.fengluan.product.remote;

import com.fengluan.spi.brand.BrandApi;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 商品服务消费品牌契约（extends BrandApi），异常走 fallback 降级
 */
@FeignClient(name = "goods-store-brand-api", path = "/brand/api",
        fallbackFactory = BrandClientFallbackFactory.class)
public interface BrandRemoteClient extends BrandApi {

    @GetMapping("/page")
    PageVO<BrandVO> page(@SpringQueryMap BrandQueryRequest query);
}