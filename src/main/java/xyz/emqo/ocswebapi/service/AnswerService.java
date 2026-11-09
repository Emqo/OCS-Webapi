/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: AnswerService
 * @Description:
 */
package xyz.emqo.ocswebapi.service;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import xyz.emqo.ocswebapi.config.DeepSeekConfig;
import xyz.emqo.ocswebapi.dto.ApiResponse;
import xyz.emqo.ocswebapi.dto.DeepSeekRequest;
import xyz.emqo.ocswebapi.dto.DeepSeekResponse;
import xyz.emqo.ocswebapi.dto.SearchRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜题服务: 调用 DeepSeek 官方 API(OpenAI 兼容协议)作答
 */
@Slf4j
@Service
public class AnswerService {

    private static final String DEFAULT_KEY_PLACEHOLDER = "REPLACE_WITH_YOUR_KEY";

    private final DeepSeekConfig deepSeekConfig;
    private final WebClient webClient;
    private final Cache<String, ApiResponse> answerCache;

    public AnswerService(DeepSeekConfig deepSeekConfig) {
        this.deepSeekConfig = deepSeekConfig;
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(deepSeekConfig.getTimeoutSeconds()));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        DeepSeekConfig.Cache cacheConfig = deepSeekConfig.getCache();
        this.answerCache = cacheConfig != null && cacheConfig.isEnabled()
                ? Caffeine.newBuilder()
                        .maximumSize(cacheConfig.getMaxSize())
                        .expireAfterWrite(Duration.ofHours(cacheConfig.getExpireHours()))
                        .build()
                : null;
    }

    /**
     * 搜索答案
     */
    public ApiResponse searchAnswer(SearchRequest request) {
        try {
            if (deepSeekConfig.getKey() == null || deepSeekConfig.getKey().isBlank()
                    || DEFAULT_KEY_PLACEHOLDER.equals(deepSeekConfig.getKey())) {
                return fail("未配置 DeepSeek API Key, 请在 application.yml 的 deepseek.api.key 中填入");
            }
            if (request.getTitle() == null || request.getTitle().isBlank()) {
                return fail("题目内容不能为空");
            }

            String type = normalizeType(request.getType());

            String cacheKey = buildCacheKey(type, request);
            ApiResponse cached = answerCache != null ? answerCache.getIfPresent(cacheKey) : null;
            if (cached != null) {
                log.info("缓存命中, 题目: {}", request.getTitle());
                return cached;
            }

            // 调用DeepSeek API
            String answer = callDeepSeekApi(buildMessages(type, request));

            ApiResponse response = ApiResponse.builder()
                    .code(1)
                    .message("成功")
                    .data(ApiResponse.ResponseData.builder()
                            .question(request.getTitle())
                            .answer(answer)
                            .build())
                    .build();
            if (answerCache != null) {
                answerCache.put(cacheKey, response);
            }
            return response;

        } catch (Exception e) {
            log.error("搜索答案失败", e);
            return fail("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 归一化题目类型, 兼容 OCS 字符串枚举与数字形式
     */
    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "other";
        }
        return switch (raw.trim().toLowerCase()) {
            case "single", "1", "单选", "单选题" -> "single";
            case "multiple", "2", "多选", "多选题" -> "multiple";
            case "judgement", "judge", "3", "判断", "判断题" -> "judgement";
            case "completion", "4", "填空", "填空题", "简答", "简答题" -> "completion";
            default -> "other";
        };
    }

    /**
     * 获取题目类型描述
     */
    private String getTypeDescription(String type) {
        return switch (type) {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "judgement" -> "判断题";
            case "completion" -> "填空题";
            default -> "其他";
        };
    }

    /**
     * 构建对话消息: system 严格约束输出格式, user 放题目与选项
     */
    private List<DeepSeekRequest.Message> buildMessages(String type, SearchRequest request) {
        String rule = switch (type) {
            case "single" -> "只需输出正确选项的字母(例如:A), 不要输出任何解释或其他内容。";
            case "multiple" -> "只需输出正确选项的字母, 多个答案用#号分隔(例如:A#C), 不要输出任何解释或其他内容。";
            case "judgement" -> "只需输出 正确 或 错误, 不要输出任何解释或其他内容。";
            default -> "只需输出简短准确的答案本身, 不要输出任何解释或其他内容。";
        };

        StringBuilder user = new StringBuilder();
        user.append("题目类型:").append(getTypeDescription(type)).append("\n");
        user.append("题目:").append(request.getTitle().trim());
        if (request.getOptions() != null && !request.getOptions().isBlank()) {
            user.append("\n选项:\n").append(request.getOptions().trim());
        }

        List<DeepSeekRequest.Message> messages = new ArrayList<>();
        messages.add(DeepSeekRequest.Message.builder()
                .role("system")
                .content("你是一个网课题库答题助手。" + rule)
                .build());
        messages.add(DeepSeekRequest.Message.builder()
                .role("user")
                .content(user.toString())
                .build());
        return messages;
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekApi(List<DeepSeekRequest.Message> messages) {
        // 构建请求体(thinking 模式下 temperature 无效, 不发送)
        DeepSeekRequest.DeepSeekRequestBuilder builder = DeepSeekRequest.builder()
                .model(deepSeekConfig.getModel())
                .messages(messages)
                .maxTokens(deepSeekConfig.getMaxTokens())
                .stream(false);
        if (deepSeekConfig.isThinking()) {
            builder.thinking(DeepSeekRequest.Thinking.builder().type("enabled").build());
        } else {
            builder.temperature(deepSeekConfig.getTemperature());
        }
        DeepSeekRequest request = builder.build();

        log.info("调用DeepSeek API, 请求内容: {}", JSON.toJSONString(request));

        DeepSeekResponse response;
        try {
            response = webClient.post()
                    .uri(deepSeekConfig.getUrl())
                    .header("Authorization", "Bearer " + deepSeekConfig.getKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DeepSeekResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException(extractApiError(e), e);
        }

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()
                || response.getChoices().get(0).getMessage() == null
                || response.getChoices().get(0).getMessage().getContent() == null
                || response.getChoices().get(0).getMessage().getContent().isBlank()) {
            throw new RuntimeException("API调用失败或返回为空");
        }

        DeepSeekResponse.Choice choice = response.getChoices().get(0);
        log.info("DeepSeek返回答案: {}, finish_reason: {}", choice.getMessage().getContent(), choice.getFinishReason());
        if ("length".equals(choice.getFinishReason())) {
            log.warn("回答因达到 max_tokens 上限被截断, 可调大 deepseek.api.max-tokens");
        }

        return cleanAnswer(choice.getMessage().getContent());
    }

    /**
     * 解析 DeepSeek 的 OpenAI 风格错误响应
     */
    private String extractApiError(WebClientResponseException e) {
        try {
            DeepSeekResponse body = JSON.parseObject(e.getResponseBodyAsString(), DeepSeekResponse.class);
            if (body != null && body.getError() != null && body.getError().getMessage() != null) {
                return "DeepSeek API错误: " + body.getError().getMessage();
            }
        } catch (JSONException ignored) {
            // 非 JSON 错误体, 回退到状态码
        }
        return "DeepSeek API调用失败: HTTP " + e.getStatusCode().value();
    }

    /**
     * 清洗答案: 去首尾空白/星号/引号与"答案:"类前缀
     */
    private String cleanAnswer(String answer) {
        String cleaned = answer.trim();
        cleaned = cleaned.replaceAll("^[\\*\\s]+|[\\*\\s]+$", "");
        cleaned = cleaned.replaceFirst("^(正确答案|答案)\\s*[:：]", "").trim();
        cleaned = cleaned.replaceFirst("^(正确答案|答案)(是|为)", "").trim();
        cleaned = cleaned.replaceAll("^[\"'“”]+|[\"'“”]+$", "");
        return cleaned.trim();
    }

    /**
     * 构建缓存 key: 类型|题目|选项
     */
    private String buildCacheKey(String type, SearchRequest request) {
        String options = request.getOptions() == null ? "" : request.getOptions().trim();
        return type + "|" + request.getTitle().trim() + "|" + options;
    }

    private ApiResponse fail(String message) {
        return ApiResponse.builder().code(0).message(message).build();
    }

}
