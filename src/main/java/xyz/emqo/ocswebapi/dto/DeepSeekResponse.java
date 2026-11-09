/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: DeepSeekResponse
 * @Description:
 */
package xyz.emqo.ocswebapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DeepSeek API响应体(OpenAI 兼容协议)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeepSeekResponse {

    /**
     * 候选回答列表
     */
    private List<Choice> choices;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 错误信息(非 2xx 响应时返回)
     */
    private Error error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        /**
         * 候选序号
         */
        private Integer index;

        /**
         * 回答内容
         */
        private Message message;

        /**
         * 完成原因: stop, length, content_filter 等
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        /**
         * 角色: assistant, tool 等
         */
        private String role;

        /**
         * 回答文本
         */
        private String content;

        /**
         * 深度思考内容(thinking 模式下返回)
         */
        @JsonProperty("reasoning_content")
        private String reasoningContent;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        /**
         * 输入Token数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 输出Token数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总Token数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        /**
         * 错误描述
         */
        private String message;

        /**
         * 错误类型
         */
        private String type;

        /**
         * 错误码
         */
        private String code;
    }

}
