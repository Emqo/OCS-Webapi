/**
 * @Author: Emqo
 * @Date: 2025/10/14 20:19
 * @FileName: QwenAnswerService
 * @Description:
 */
package xyz.emqo.qwenxuexitong.service;


import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import xyz.emqo.qwenxuexitong.config.QwenConfig;
import xyz.emqo.qwenxuexitong.dto.ApiResponse;
import xyz.emqo.qwenxuexitong.dto.QwenRequest;
import xyz.emqo.qwenxuexitong.dto.QwenResponse;
import xyz.emqo.qwenxuexitong.dto.SearchRequest;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class QwenAnswerService {

    private final QwenConfig qwenConfig;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 搜索答案
     */
    public ApiResponse searchAnswer(SearchRequest request) {
        try {
            // 构建提示词
            String prompt = buildPrompt(request);
            log.info("生成的提示词: {}", prompt);

            // 调用通义千问API
            String answer = callQwenApi(prompt);

            return ApiResponse.builder()
                    .code(1)
                    .message("成功")
                    .data(ApiResponse.ResponseData.builder()
                            .question(request.getTitle())
                            .answer(answer)
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("搜索答案失败", e);
            return ApiResponse.builder()
                    .code(0)
                    .message("搜索失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(SearchRequest request) {
        StringBuilder prompt = new StringBuilder();

        // 根据题目类型构建不同的提示词
        String typeDesc = getTypeDescription(request.getType());

        prompt.append("你是一个专业的题目解答助手。请根据以下信息回答问题：\n\n");
        prompt.append("题目类型：").append(typeDesc).append("\n");
        prompt.append("题目：").append(request.getTitle()).append("\n");

        if (request.getOptions() != null && !request.getOptions().isEmpty()) {
            prompt.append("选项：\n").append(request.getOptions()).append("\n");
        }

        prompt.append("\n请直接给出答案，");

        if ("multiple".equals(request.getType())) {
            prompt.append("如果是多选题，请用#号分隔多个答案（例如：A#B#C）。");
        } else if ("single".equals(request.getType())) {
            prompt.append("只需要给出选项字母（例如：A 或 B）。");
        } else if ("judgement".equals(request.getType())) {
            prompt.append("只需要回答'正确'或'错误'。");
        } else {
            prompt.append("给出简洁明确的答案。");
        }

        return prompt.toString();
    }

    /**
     * 获取题目类型描述
     */
    private String getTypeDescription(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "judgement" -> "判断题";
            case "completion" -> "填空题";
            default -> "其他";
        };
    }

    /**
     * 调用通义千问API
     */
    private String callQwenApi(String prompt) {
        // 构建请求体
        QwenRequest request = QwenRequest.builder()
                .model(qwenConfig.getModel())
                .input(QwenRequest.Input.builder()
                        .messages(Collections.singletonList(
                                QwenRequest.Message.builder()
                                        .role("user")
                                        .content(prompt)
                                        .build()
                        ))
                        .build())
                .parameters(QwenRequest.Parameters.builder()
                        .resultFormat("text")
                        .build())
                .build();

        log.info("调用通义千问API，请求内容: {}", JSON.toJSONString(request));

        // 发送HTTP请求
        Mono<QwenResponse> responseMono = webClient.post()
                .uri(qwenConfig.getUrl())
                .header("Authorization", "Bearer " + qwenConfig.getKey())
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(QwenResponse.class);

        // 获取响应
        QwenResponse response = responseMono.block();

        if (response != null && response.getOutput() != null) {
            String answer = response.getOutput().getText();
            log.info("通义千问返回答案: {}", answer);
            return answer.trim();
        }

        throw new RuntimeException("API调用失败或返回为空");
    }

}