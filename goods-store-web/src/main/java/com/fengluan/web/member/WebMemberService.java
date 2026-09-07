package com.fengluan.web.member;

import com.fengluan.spi.member.dto.MemberAddressRequest;
import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberAddressVO;
import com.fengluan.spi.member.vo.MemberPageVO;
import com.fengluan.spi.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员聚合服务（BFF）：个人中心 = 资料 + 地址列表一次编排
 */
@Service
@RequiredArgsConstructor
public class WebMemberService {

    private final MemberFeignClient memberFeignClient;

    /** 会员分页列表（管理端） */
    public MemberPageVO page(MemberQueryRequest query) {
        return memberFeignClient.page(query);
    }

    /** 个人中心聚合：资料 + 地址列表 */
    public Map<String, Object> profile(Long memberId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", memberFeignClient.getProfile(memberId));
        data.put("addresses", memberFeignClient.listAddress(memberId));
        return data;
    }

    public MemberVO updateProfile(Long memberId, MemberProfileUpdateRequest request) {
        return memberFeignClient.updateProfile(memberId, request);
    }

    public MemberVO setEnabled(Long memberId, Boolean enabled) {
        return memberFeignClient.setEnabled(memberId, enabled);
    }

    public List<MemberAddressVO> listAddress(Long memberId) {
        return memberFeignClient.listAddress(memberId);
    }

    public MemberAddressVO createAddress(Long memberId, MemberAddressRequest request) {
        return memberFeignClient.createAddress(memberId, request);
    }

    public MemberAddressVO updateAddress(Long memberId, Long addrId, MemberAddressRequest request) {
        return memberFeignClient.updateAddress(memberId, addrId, request);
    }

    public void deleteAddress(Long memberId, Long addrId) {
        memberFeignClient.deleteAddress(memberId, addrId);
    }

    public void setDefaultAddress(Long memberId, Long addrId) {
        memberFeignClient.setDefaultAddress(memberId, addrId);
    }
}