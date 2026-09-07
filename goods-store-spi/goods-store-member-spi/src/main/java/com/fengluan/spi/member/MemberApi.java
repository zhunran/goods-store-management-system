package com.fengluan.spi.member;

import com.fengluan.spi.member.dto.MemberAddressRequest;
import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberAddressVO;
import com.fengluan.spi.member.vo.MemberPageVO;
import com.fengluan.spi.member.vo.MemberVO;
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
 * 会员服务纯 HTTP 契约（Day 8 起规范：spi 禁 @FeignClient，controller implements、消费方 extends 复用）
 * 相对路径基于实现方 Controller 类级前缀 /member/api，完整路径见实现类。
 * 越权语义：id 为被操作会员，消费方经网关注入 X-User-Id，实现方校验 id == 当前用户。
 */
public interface MemberApi {

    // —— 会员列表（管理端） ——
    /** 会员分页列表（管理端） */
    @GetMapping("/page")
    MemberPageVO page(MemberQueryRequest query);

    // —— 会员资料 ——
    /** 会员资料（脱敏返回，不暴露 password/cardId 原文） */
    @GetMapping("/{id}")
    MemberVO getProfile(@PathVariable Long id);

    /** 编辑会员资料 */
    @PutMapping("/{id}")
    MemberVO updateProfile(@PathVariable Long id, @Valid @RequestBody MemberProfileUpdateRequest request);

    /** 启用/禁用会员（管理端，enabled=false 禁用后该会员重新登录被拒） */
    @PutMapping("/{id}/enabled")
    MemberVO setEnabled(@PathVariable Long id, @RequestParam("enabled") Boolean enabled);

    // —— 收货地址 ——
    /** 地址列表 */
    @GetMapping("/{id}/address")
    List<MemberAddressVO> listAddress(@PathVariable Long id);

    /** 新增地址 */
    @PostMapping("/{id}/address")
    MemberAddressVO createAddress(@PathVariable Long id, @Valid @RequestBody MemberAddressRequest request);

    /** 编辑地址 */
    @PutMapping("/{id}/address/{addrId}")
    MemberAddressVO updateAddress(@PathVariable Long id, @PathVariable Long addrId, @Valid @RequestBody MemberAddressRequest request);

    /** 删除地址 */
    @DeleteMapping("/{id}/address/{addrId}")
    Void deleteAddress(@PathVariable Long id, @PathVariable Long addrId);

    /** 设某地址为默认（互斥，其余自动取消） */
    @PutMapping("/{id}/address/{addrId}/default")
    Void setDefaultAddress(@PathVariable Long id, @PathVariable Long addrId);

    /** 默认地址（供 trade-api 下单取收货地址复用） */
    @GetMapping("/{id}/address/default")
    MemberAddressVO getDefaultAddress(@PathVariable Long id);

    /** 查会员账号（供 seckill-api 内部建单/归属校验复用，无越权校验，调用方经网关层已有会话鉴权） */
    @GetMapping("/{id}/account")
    String getAccount(@PathVariable Long id);
}