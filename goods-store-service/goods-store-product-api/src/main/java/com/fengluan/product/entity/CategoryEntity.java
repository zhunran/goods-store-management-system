package com.fengluan.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("category")
public class CategoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 父类别编号 */
    private Integer parentId;
    /** 类别名称 */
    private String name;
    /** 类别标题 */
    private String title;
    /** 类别标签 */
    private String tag;
    /** 类别样式 */
    private String icon;
    /** 类别摘要 */
    private String summary;
    /** 排序号 */
    private Integer sort;
    /** 备注 */
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