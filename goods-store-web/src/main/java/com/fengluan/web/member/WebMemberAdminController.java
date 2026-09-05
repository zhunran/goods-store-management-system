package com.fengluan.web.member;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.member.dto.MemberQueryRequest;
import com.fengluan.spi.member.vo.MemberPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
