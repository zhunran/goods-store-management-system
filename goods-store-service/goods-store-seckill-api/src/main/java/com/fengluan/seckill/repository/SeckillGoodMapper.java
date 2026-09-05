package com.fengluan.seckill.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.seckill.entity.SeckillGoodEntity;
import com.fengluan.spi.seckill.vo.SeckillGoodVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeckillGoodMapper extends BaseMapper<SeckillGoodEntity> {

    /** 当前有效活动窗口内的秒杀商品（enabled=1 且 start<=now<=end 且商品未删） */
    @Select("""
        SELECT sg.id, sg.good_id, sg.description, g.name AS goodName, g.pic AS goodPic,
               g.price AS originalPrice,
               s.id AS seckillId, s.name AS activityName,
               s.start_time AS startTime, s.end_time AS endTime
        FROM seckill_good sg
        JOIN seckill s ON sg.seckill_id = s.id
        JOIN good g ON sg.good_id = g.id
        WHERE s.enabled = 1
          AND s.start_time <= #{now}
          AND s.end_time >= #{now}
          AND g.is_del = 0
        ORDER BY sg.id
        """)
    List<SeckillGoodVO> selectActiveSeckillGoods(@Param("now") LocalDateTime now);

    /** 按活动查询其下全部秒杀商品（管理端，不限时间窗口与启用状态，含已下架商品） */
    @Select("""
        SELECT sg.id, sg.good_id, sg.description, g.name AS goodName, g.pic AS goodPic,
               g.price AS originalPrice,
               s.id AS seckillId, s.name AS activityName,
               s.start_time AS startTime, s.end_time AS endTime
        FROM seckill_good sg
        JOIN seckill s ON sg.seckill_id = s.id
        JOIN good g ON sg.good_id = g.id
        WHERE sg.seckill_id = #{seckillId}
          AND g.is_del = 0
        ORDER BY sg.id
        """)
    List<SeckillGoodVO> selectByActivity(@Param("seckillId") Long seckillId);
}