package com.fengluan.spi.product.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodCreateRequest {
    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;
    /** 别名 */
    private String alias;
    /** 摘要 */
    private String summary;
    /** 分类 id */
    private Integer categoryId;
    /** 品牌 id */
    private Integer brandId;
    /** 建议售价（划线价） */
    private BigDecimal markPrice;
    /** 实售价 */
    private BigDecimal price;
    /** 库存数量 */
    private Integer qty;
    /** 主图 */
    private String pic;
    /** 次图 */
    private String pic2;
    /** 详情 */
    private String detail;
    /** 是否下架 */
    private Boolean isTakeDown;
    /** 是否热销 */
    private Boolean isHot;
    /** 详情图列表（一次性提交，按序写入 good_detail_pics） */
    private List<String> detailPicList;
}