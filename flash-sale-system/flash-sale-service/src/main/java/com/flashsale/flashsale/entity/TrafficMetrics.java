package com.flashsale.flashsale.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("traffic_metrics")
public class TrafficMetrics {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long activityId;
    private String interfaceName;
    private Integer qps;
    private Integer predictedPeakQps;
    private LocalDateTime collectTime;
}
