package com.fengluan.web.region;

import com.fengluan.spi.member.RegionApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * web 层行政区划 Feign 客户端：复用 member 纯契约，指向 member-api 的 /region/api。
 * 与 MemberFeignClient 指向同一服务，需用独立 contextId 避免 Bean 命名冲突。
 */
@FeignClient(name = "goods-store-member-api", contextId = "regionFeignClient", path = "/region/api")
public interface RegionFeignClient extends RegionApi {
}