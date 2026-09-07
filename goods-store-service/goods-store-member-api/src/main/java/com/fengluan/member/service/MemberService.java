package com.fengluan.member.service;

import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberPageVO;
import com.fengluan.spi.member.vo.MemberVO;

public interface MemberService {

    /** 会员分页列表（管理端） */
    MemberPageVO page(MemberQueryRequest query);

    /** 会员资料 */
    MemberVO getProfile(Long id);

    /** 会员账号（服务间内部查询，无脱敏必要） */
    String getAccount(Long id);

    /** 编辑会员资料 */
    MemberVO updateProfile(Long id, MemberProfileUpdateRequest request);

    /** 启用/禁用会员（管理端） */
    MemberVO setEnabled(Long id, Boolean enabled);
}