package com.fengluan.seckill.remote;

import com.fengluan.spi.member.MemberApi;
import org.springframework.cloud.openfeign.FeignClient;

/** seckill 消费 member：仅 extends 契约（取会员账户用于订单归属） */
@FeignClient(name = "goods-store-member-api", contextId = "seckillMemberClient", path = "/member/api")
public interface SeckillMemberClient extends MemberApi {
}