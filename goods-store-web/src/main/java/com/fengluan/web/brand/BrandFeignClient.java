package com.fengluan.web.brand;

import com.fengluan.spi.brand.BrandApi;
import com.fengluan.spi.brand.dto.BrandQueryRequest;
import com.fengluan.spi.brand.vo.BrandVO;
import com.fengluan.spi.brand.vo.PageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * web 层品牌 Feign 客户端：extends brand 纯契约
 */
@FeignClient(name = "goods-store-brand-api", path = "/brand/api")
public interface BrandFeignClient extends BrandApi {

    /**
     * GET 请求的 POJO 参数必须用 @SpringQueryMap 展开为 query 参数，
     * 否则 Feign 会将其当作请求体发送，下游无法正确绑定。
     */
    @GetMapping("/page")
    PageVO<BrandVO> page(@SpringQueryMap BrandQueryRequest query);
}
