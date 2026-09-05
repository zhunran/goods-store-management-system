package com.fengluan.product.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengluan.product.entity.GoodEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface GoodMapper extends BaseMapper<GoodEntity> {

    /**
     * 乐观锁扣减库存：qty >= count 才执行，原子防超卖，影响行数 0 表示库存不足
     */
    @Update("UPDATE good SET qty = qty - #{count} WHERE id = #{goodId} AND qty >= #{count}")
    int deductStock(@Param("goodId") Long goodId, @Param("count") Integer count);

    /**
     * 恢复库存
     */
    @Update("UPDATE good SET qty = qty + #{count} WHERE id = #{goodId}")
    int restoreStock(@Param("goodId") Long goodId, @Param("count") Integer count);
}
