package com.flashsale.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.inventory.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockMapper extends BaseMapper<Stock> {

    @Update("UPDATE stock SET sold_count = sold_count + #{qty}, version = version + 1 "
            + "WHERE product_id = #{pid} AND (total_stock - sold_count) >= #{qty} AND version = #{ver}")
    int lockStock(@Param("pid") Long pid, @Param("qty") Integer qty, @Param("ver") Integer ver);

    @Update("UPDATE stock SET sold_count = sold_count + #{qty} "
            + "WHERE product_id = #{pid} AND (total_stock - sold_count) >= #{qty}")
    int addSoldCount(@Param("pid") Long pid, @Param("qty") Integer qty);

    @Update("UPDATE stock SET sold_count = sold_count - #{qty} "
            + "WHERE product_id = #{pid} AND sold_count >= #{qty}")
    int unlockStock(@Param("pid") Long pid, @Param("qty") Integer qty);

    @Update("UPDATE stock SET sold_count = sold_count - #{qty}, total_stock = total_stock - #{qty} "
            + "WHERE product_id = #{pid} AND sold_count >= #{qty}")
    int deductStock(@Param("pid") Long pid, @Param("qty") Integer qty);
}
