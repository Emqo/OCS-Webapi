/**
 * @Author: Emqo
 * @Date: 2025/10/14 20:19
 * @FileName: QwenRequest
 * @Description:
 */
package xyz.emqo.qwenxuexitong.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通义千问API请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QwenRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 输入内容
     */
    private Input input;

    /**
     * 参数配置
     */
    private Parameters parameters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Input {
        /**
         * 对话消息列表
         */
        private List<Message> messages;
    }

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
    public static class Parameters {
        /**
         * 返回格式: text 或 message
         */
        @JsonProperty("result_format")
        private String resultFormat;
    }

}