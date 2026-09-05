package com.fengluan.spi.member.dto;

import lombok.Data;

@Data
public class MemberQueryRequest {
    /** 当前页码，默认第一页 */
    private Long pageNum = 1L;
    /** 每页条数，默认10条 */
    private Long pageSize = 10L;
    /** 会员姓名/账号，模糊查询 */
    private String keyword;
}