package com.flashsale.flashsale.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * DeepSeek 大模型调用封装（AI 模块增强能力，可配置 / 可降级）。
 *
 * <p>设计约束：
 * <ul>
 *   <li><b>可配置</b>：api-url / api-key / enabled 全部来自 application.yml，Java 代码中不硬编码 Key。</li>
 *   <li><b>可降级</b>：未启用、未配置 Key、超时、网络异常、鉴权失败、响应为空 —— 一律返回 {@code null}，
 *       由调用方回退到原有规则逻辑，绝不抛异常给业务。</li>
 *   <li><b>不影响主流程</b>：本类只负责"取一段文本"，不含任何业务判断；秒杀下单主链路不调用本类。</li>
 *   <li><b>安全</b>：日志只打印耗时与字符数，不打印 API Key。</li>
 * </ul>
 */
@Slf4j
@Component
public class DeepSeekService {

    /** 模型固定为 deepseek-chat */
    private static final String MODEL = "deepseek-chat";
    /** 超时 3 秒（连接 + 读取各自 3 秒） */
    private static final int TIMEOUT_MS = 3000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.enabled:false}")
    private boolean enabled;

    public DeepSeekService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    /** 是否具备可用配置（启用 且 api-key 非空） */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * 发起一次对话补全请求。
     *
     * @return 模型回答文本；未配置 / 调用失败 / 响应不可解析时返回 {@code null}
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            log.debug("DeepSeek 未启用或未配置 api-key，跳过调用，使用规则结果");
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            Map<String, Object> body = Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)),
                    "temperature", 0.2,
                    "stream", false);

            long start = System.currentTimeMillis();
            String raw = restTemplate.postForObject(apiUrl, new HttpEntity<>(body, headers), String.class);
            long cost = System.currentTimeMillis() - start;

            String content = extractContent(raw);
            if (content == null || content.isBlank()) {
                log.warn("DeepSeek 返回内容为空, 降级为规则结果, cost={}ms", cost);
                return null;
            }
            log.info("DeepSeek 调用成功: cost={}ms, 返回{}字", cost, content.length());
            return content.trim();
        } catch (Exception e) {
            // 含超时 / 401 / 403 / DNS / 限流等，统一降级，不影响业务
            log.warn("DeepSeek 调用失败，降级为规则结果: {}", e.getMessage());
            return null;
        }
    }

    /** 从 OpenAI 兼容响应中提取 choices[0].message.content */
    private String extractContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode choices = objectMapper.readTree(raw).path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return null;
            }
            return choices.get(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("DeepSeek 响应解析失败, 降级为规则结果: {}", e.getMessage());
            return null;
        }
    }
}
