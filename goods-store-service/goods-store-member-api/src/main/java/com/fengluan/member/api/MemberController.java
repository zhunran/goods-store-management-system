package com.fengluan.member.api;

import com.fengluan.member.service.MemberAddressService;
import com.fengluan.member.service.MemberService;
import com.fengluan.member.util.CurrentUserUtil;
import com.fengluan.spi.member.MemberApi;
import com.fengluan.spi.member.dto.MemberAddressRequest;
import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberAddressVO;
import com.fengluan.spi.member.vo.MemberPageVO;
import com.fengluan.spi.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员服务实现：implements MemberApi 纯契约
 * 类级前缀 /member/api，与网关路由一致。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/member/api")
public class MemberController implements MemberApi {

    private final MemberService memberService;
    private final MemberAddressService addressService;

    @Override
    public MemberPageVO page(MemberQueryRequest query) {
        return memberService.page(query);
    }

    @Override
    public MemberVO getProfile(Long id) {
        CurrentUserUtil.assertOwned(id);
        return memberService.getProfile(id);
    }

    @Override
    public String getAccount(Long id) {
        // 供 seckill 等服务内部建单/归属校验复用，不做本站越权校验（调用方在网关层已有会话鉴权）
        return memberService.getAccount(id);
    }

    @Override
    public MemberVO updateProfile(Long id, MemberProfileUpdateRequest request) {
        CurrentUserUtil.assertOwned(id);
        return memberService.updateProfile(id, request);
    }

    @Override
    public MemberVO setEnabled(Long id, Boolean enabled) {
        // 管理端操作，无本站越权约束
        return memberService.setEnabled(id, enabled);
    }

    @Override
    public List<MemberAddressVO> listAddress(Long id) {
        CurrentUserUtil.assertOwned(id);
        return addressService.listAddress(id);
    }

    @Override
    public MemberAddressVO createAddress(Long id, MemberAddressRequest request) {
        CurrentUserUtil.assertOwned(id);
        return addressService.createAddress(id, request);
    }

    @Override
    public MemberAddressVO updateAddress(Long id, Long addrId, MemberAddressRequest request) {
        CurrentUserUtil.assertOwned(id);
        return addressService.updateAddress(id, addrId, request);
    }

    @Override
    public Void deleteAddress(Long id, Long addrId) {
        CurrentUserUtil.assertOwned(id);
        addressService.deleteAddress(id, addrId);
        return null;
    }

    @Override
    public Void setDefaultAddress(Long id, Long addrId) {
        CurrentUserUtil.assertOwned(id);
        addressService.setDefaultAddress(id, addrId);
        return null;
    }

    @Override
    public MemberAddressVO getDefaultAddress(Long id) {
        // 供 trade 内部下单使用，不做本站越权校验（调用方在网关层已有会话鉴权）
        return addressService.getDefaultAddress(id);
    }
}