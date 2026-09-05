package com.fengluan.spi.member.vo;

import lombok.Data;

import java.util.List;

/** 会员列表分页返回（spi 自建，不依赖 common） */
@Data
public class MemberPageVO {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private List<MemberVO> records;
}
