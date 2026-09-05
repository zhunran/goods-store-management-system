package com.fengluan.trade.remote;

import com.fengluan.spi.member.MemberApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 交易服务消费会员契约（extends MemberApi），取账号/默认地址
 */
@FeignClient(name = "goods-store-member-api", path = "/member/api")
public interface TradeMemberClient extends MemberApi {
}