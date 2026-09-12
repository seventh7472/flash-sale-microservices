package com.flashsale.flashsale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flashsale.flashsale.entity.TrafficMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TrafficMetricsMapper extends BaseMapper<TrafficMetrics> {

    @Select("SELECT qps FROM traffic_metrics WHERE activity_id = #{activityId} ORDER BY collect_time DESC LIMIT #{limit}")
    List<Integer> findRecentQpsByActivityId(@Param("activityId") Long activityId, @Param("limit") int limit);

    @Select("SELECT * FROM traffic_metrics WHERE collect_time >= #{since} AND activity_id = #{activityId} ORDER BY collect_time ASC")
    List<TrafficMetrics> findRecentMetricsByActivity(@Param("activityId") Long activityId, @Param("since") LocalDateTime since);

    @Select("SELECT * FROM traffic_metrics WHERE collect_time >= #{since} AND activity_id IS NULL ORDER BY collect_time ASC")
    List<TrafficMetrics> findRecentMetricsGlobal(@Param("since") LocalDateTime since);

    @Select("SELECT * FROM traffic_metrics WHERE collect_time >= #{since} ORDER BY collect_time ASC")
    List<TrafficMetrics> findRecentMetricsSince(@Param("since") LocalDateTime since);

    @Select("SELECT COALESCE(SUM(qps), 0) FROM traffic_metrics WHERE interface_name = #{interfaceName} AND collect_time >= #{since}")
    int sumQpsSince(@Param("interfaceName") String interfaceName, @Param("since") String since);
}
