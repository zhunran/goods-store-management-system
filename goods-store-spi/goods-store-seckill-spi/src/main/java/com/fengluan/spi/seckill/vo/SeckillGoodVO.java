package com.fengluan.spi.seckill.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户端秒杀商品展示 */
@Data
public class SeckillGoodVO {
    private Long id;
    private Long goodId;
    private String goodName;
    private String goodPic;
    private BigDecimal originalPrice;
    /** 秒杀价 */
    private BigDecimal seckillPrice;
    /** 限量库存 */
    private Integer stockCount;
    /** DB 已售 */
    private Integer stockSold;
    private String description;
    // 活动窗口信息（由 service 填充）
    private Long seckillId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;       // NOT_STARTED / IN_PROGRESS / ENDED
    private Long countdownSec;   // 倒计时（秒），未开始>0，进行中=0
    // 抢购状态（由 service 从 Redis 填充）
    private Long stockLeft;      // 剩余库存（Redis 预热后有效；null=未预热不可知，不算售空）
    private Boolean robbed;      // 当前用户是否已抢到（Redis 防重键判定）
}