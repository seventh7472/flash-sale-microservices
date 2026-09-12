package com.flashsale.flashsale.model;

import lombok.Data;

@Data
public class CheatUser {
    private Long userId;
    private String username;
    private String ip;
    private Integer requestCount;
    private Integer riskScore;
    private Boolean banned;
    /** 规则引擎判定理由（检测时写入，作为 aiAnalysis 缺失时的兜底展示） */
    private String reason;
    /** DeepSeek 二次分析的自然语言风险说明；未生成或调用失败时为 null */
    private String aiAnalysis;
}
