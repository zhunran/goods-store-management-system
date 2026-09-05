package com.fengluan.common.dto;

import lombok.Data;

@Data
public class PageQuery {
    //页码
    private  Integer pageNo=1;
    //每页条数
    private  Integer pageNum=10;
    //排序字段
    private String sortField;
    //排序方向
    private String sortOrder="desc";
}
