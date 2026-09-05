package com.fengluan.spi.product.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GoodVO {
    private Long id;
    /** SPU 编号 */
    private String spuNo;
    /** 商品名称 */
    private String name;
    /** 别名 */
    private String alias;
    /** 摘要 */
    private String summary;
    /** 分类 id */
    private Integer categoryId;
    /** 品牌 id */
    private Integer brandId;
    /** 是否热销 */
    private Boolean isHot;
    /** 是否逻辑删除 */
    private Boolean isDel;
    /** 是否参与秒杀 */
    private Boolean isSeckill;
    /** 库存（trade 购物车/下单校验用） */
    private Integer qty;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 最后修改时间 */
    private LocalDateTime updatedTime;

    /** 市场价格（划线价） */
    private BigDecimal markPrice;
    /** 销售价格 */
    private BigDecimal price;
    /** 主图 */
    private String pic;
    /** 品牌名称（由 brand Feign 填充，异常降级为 "-"） */
    private String brandName;
    /** 分类名称 */
    private String categoryName;
    /** 详情图列表（good_detail_pics 表按 sort 升序） */
    private List<String> detailPicList;
}