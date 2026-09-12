package com.flashsale.flashsale.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 风控助手对话回答。
 *
 * <p>{@code degraded=true} 表示 DeepSeek 未启用 / 未配置 / 调用失败，本次返回的是
 * 基于系统实时数据拼装的降级摘要，而非大模型生成内容。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReply {

    /** 回答正文（自然语言） */
    private String answer;

    /** 是否降级回答（true = 未经大模型生成） */
    private boolean degraded;
}
