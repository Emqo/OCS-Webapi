/**
 * @Author: Emqo
 * @Date: 2025/10/14 20:19
 * @FileName: QwenResponse
 * @Description:
 */
package xyz.emqo.qwenxuexitong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 通义千问API响应体
 */
@Data
public class QwenResponse {

    /**
     * 输出内容
     */
    private Output output;

    /**
     * Token使用情况
     */
    private Usage usage;

    /**
     * 请求ID
     */
    @JsonProperty("request_id")
    private String requestId;

    @Data
    public static class Output {
        /**
         * 生成的文本内容
         */
        private String text;

        /**
         * 完成原因
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Usage {
        /**
         * 输出Token数
         */
        @JsonProperty("output_tokens")
        private Integer outputTokens;

        /**
         * 输入Token数
         */
        @JsonProperty("input_tokens")
        private Integer inputTokens;
    }

}