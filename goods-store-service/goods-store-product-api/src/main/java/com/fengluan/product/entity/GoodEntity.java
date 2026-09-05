package com.fengluan.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("good")
public class GoodEntity {
    @TableId(type = IdType.AUTO)
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
    /** 建议售价 */
    private BigDecimal markPrice;
    /** 实售价 */
    private BigDecimal price;
    /** 库存数量 */
    private Integer qty;
    /** 主图 */
    private String pic;
    /** 次图 */
    private String pic2;
    /** 详情图 */
    private String detailPics;
    /** 详情 */
    private String detail;
    /** 是否下架 */
    private Boolean isTakeDown;
    /** 是否热销 */
    private Boolean isHot;
    /** 是否逻辑删除 */
    private Boolean isDel;
    /** 是否参与秒杀 */
    private Boolean isSeckill;
    /** 描述 */
    private String description;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 创建人 */
    private String createdBy;
    /** 最后修改时间 */
    private LocalDateTime updatedTime;
    /** 最后修改人 */
    private String updatedBy;
}