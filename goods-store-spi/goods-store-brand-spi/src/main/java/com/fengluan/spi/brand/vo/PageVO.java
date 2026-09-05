package com.fengluan.spi.brand.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 纯 POJO 分页对象（spi 不依赖 mybatis/common，故不用 IPage）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> {
    /** 总记录数 */
    private Long total;
    /** 当前页数据 */
    private List<T> records;

    public static <T> PageVO<T> empty() {
        return new PageVO<>(0L, Collections.emptyList());
    }
}