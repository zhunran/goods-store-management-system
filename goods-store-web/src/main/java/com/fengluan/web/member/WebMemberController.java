package com.fengluan.web.member;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.member.dto.MemberAddressRequest;
import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.vo.MemberAddressVO;
import com.fengluan.spi.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * web 层会员聚合入口：前端唯一入口 /app/api/**
 * 当前用户来自网关注入的 X-User-Id，作为被操作会员 id。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/member")
public class WebMemberController {

    private final WebMemberService webMemberService;

    @GetMapping("/profile")
    public ApiResult<Map<String, Object>> profile(@RequestHeader(value = "X-User-Id", required = false) Long memberId) {
        return ApiResult.success(webMemberService.profile(memberId));
    }

    @PutMapping("/profile")
    public ApiResult<MemberVO> updateProfile(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                             @RequestBody MemberProfileUpdateRequest request) {
        return ApiResult.success(webMemberService.updateProfile(memberId, request));
    }

    @PostMapping("/address")
    public ApiResult<MemberAddressVO> createAddress(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                                    @RequestBody MemberAddressRequest request) {
        return ApiResult.success(webMemberService.createAddress(memberId, request));
    }

    @DeleteMapping("/address/{addrId}")
    public ApiResult<Void> deleteAddress(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                         @PathVariable Long addrId) {
        webMemberService.deleteAddress(memberId, addrId);
        return ApiResult.success();
    }

    @PutMapping("/address/{addrId}/default")
    public ApiResult<Void> setDefaultAddress(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                             @PathVariable Long addrId) {
        webMemberService.setDefaultAddress(memberId, addrId);
        return ApiResult.success();
    }

    @PutMapping("/address/{addrId}")
    public ApiResult<MemberAddressVO> updateAddress(@RequestHeader(value = "X-User-Id", required = false) Long memberId,
                                                    @PathVariable Long addrId,
                                                    @RequestBody MemberAddressRequest request) {
        return ApiResult.success(webMemberService.updateAddress(memberId, addrId, request));
    }
}