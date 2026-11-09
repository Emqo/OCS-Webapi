/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: DeepSeekRequest
 * @Description:
 */
package xyz.emqo.ocswebapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DeepSeek API请求体(OpenAI 兼容协议)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeepSeekRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 对话消息列表
     */
    private List<Message> messages;

    /**
     * 采样温度(thinking 模式下不发送)
     */
    private Double temperature;

    /**
     * 单次回答最大 token 数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /**
     * 是否流式返回
     */
    private Boolean stream;

    /**
     * 深度思考模式开关
     */
    private Thinking thinking;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色: system, user, assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Thinking {
        /**
         * enabled-开启深度思考, disabled-关闭
         */
        private String type;
    }

}
