package com.fengluan.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("good_detail_pics")
public class GoodDetailPicsEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 商品编号 */
    private Integer goodId;
    /** 细节图地址 */
    private String url;
    /** 显示顺序 */
    private Integer sort;
    /** 备注 */
    private String description;
}