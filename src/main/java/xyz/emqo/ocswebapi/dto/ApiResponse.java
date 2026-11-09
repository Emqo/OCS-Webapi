/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: ApiResponse
 * @Description:
 */
package xyz.emqo.ocswebapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API统一响应格式
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {

    /**
     * 状态码: 1-成功, 0-失败
     */
    private Integer code;

    /**
     * 消息
     */
    private String message;

    /**
     * 数据
     */
    private ResponseData data;

    /**
     * 响应数据
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResponseData {
        /**
         * 题目
         */
        private String question;

        /**
         * 答案
         */
        private String answer;
    }

}
