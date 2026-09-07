package com.fengluan.web.member;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.member.dto.MemberProfileUpdateRequest;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberPageVO;
import com.fengluan.spi.member.vo.MemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员管理聚合入口：/app/api/member/admin/**（管理端，需 admin 角色）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/app/api/member/admin")
public class WebMemberAdminController {

    private final WebMemberService webMemberService;

    @GetMapping("/page")
    public ApiResult<MemberPageVO> page(MemberQueryRequest query) {
        return ApiResult.success(webMemberService.page(query));
    }

    @PutMapping("/{id}")
    public ApiResult<MemberVO> update(@PathVariable Long id, @RequestBody MemberProfileUpdateRequest request) {
        return ApiResult.success(webMemberService.updateProfile(id, request));
    }

    @PutMapping("/{id}/enabled")
    public ApiResult<MemberVO> setEnabled(@PathVariable Long id, @RequestParam("enabled") Boolean enabled) {
        return ApiResult.success(webMemberService.setEnabled(id, enabled));
    }
}
