package com.flashsale.flashsale.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TrafficPrediction {
    private Long activityId;
    private int predictedQps;
    private int predictedPeakQps;
    private int currentInstances;
    private int recommendedInstances;
    private int confidence;
    private String analysis;
    /** DeepSeek 生成的自然语言解读；DeepSeek 不可用时由规则模板生成，保证该字段始终有值 */
    private String textAnalysis;
    private String warningLevel;
    private String trend;
    private String dataSource;
    private String scaleStatus;
    private String scaleTime;
    private List<TrafficPoint> timeSeries = new ArrayList<>();

    @Data
    public static class TrafficPoint {
        private String time;
        private Integer historyQps;
        private Integer predictedQps;

        public TrafficPoint() {
        }

        public TrafficPoint(String time, Integer historyQps, Integer predictedQps) {
            this.time = time;
            this.historyQps = historyQps;
            this.predictedQps = predictedQps;
        }
    }
}
