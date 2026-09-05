package com.fengluan.member.service;

import com.fengluan.spi.member.dto.MemberAddressRequest;
import com.fengluan.spi.member.vo.MemberAddressVO;

import java.util.List;

public interface MemberAddressService {

    /** 地址列表 */
    List<MemberAddressVO> listAddress(Long memberId);

    /** 新增地址 */
    MemberAddressVO createAddress(Long memberId, MemberAddressRequest request);

    /** 编辑地址 */
    MemberAddressVO updateAddress(Long memberId, Long addrId, MemberAddressRequest request);

    /** 删除地址 */
    void deleteAddress(Long memberId, Long addrId);

    /** 设某地址为默认（互斥） */
    void setDefaultAddress(Long memberId, Long addrId);

    /** 默认地址 */
    MemberAddressVO getDefaultAddress(Long memberId);
}