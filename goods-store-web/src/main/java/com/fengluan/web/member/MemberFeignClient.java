package com.fengluan.web.member;

import com.fengluan.spi.member.MemberApi;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberPageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * web 层会员 Feign 客户端：extends member 纯契约
 */
@FeignClient(name = "goods-store-member-api", path = "/member/api")
public interface MemberFeignClient extends MemberApi {

    /**
     * GET 请求的 POJO 参数必须用 @SpringQueryMap 展开为 query 参数。
     */
    @GetMapping("/page")
    MemberPageVO page(@SpringQueryMap MemberQueryRequest query);
    
}
