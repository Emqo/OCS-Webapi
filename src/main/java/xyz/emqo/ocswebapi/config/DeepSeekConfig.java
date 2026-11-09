/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: DeepSeekConfig
 * @Description: DeepSeek API 配置
 */
package xyz.emqo.ocswebapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek.api")
public class DeepSeekConfig {

    /**
     * DeepSeek API Key(platform.deepseek.com 申请)
     */
    private String key;

    /**
     * DeepSeek API 地址(OpenAI 兼容协议)
     */
    private String url = "https://api.deepseek.com/chat/completions";

    /**
     * 使用的模型名称: deepseek-flash / deepseek-v4-pro
     */
    private String model = "deepseek-flash";

    /**
     * 采样温度(thinking 模式下无效,不发送)
     */
    private Double temperature = 1.0;

    /**
     * 单次回答最大 token 数
     */
    private Integer maxTokens = 1024;

    /**
     * 是否开启深度思考模式(更准但更慢、更贵)
     */
    private boolean thinking = false;

    /**
     * 调用 DeepSeek 的响应超时(秒)
     */
    private int timeoutSeconds = 60;

    /**
     * 答案缓存配置
     */
    private Cache cache = new Cache();

    @Data
    public static class Cache {

        /**
         * 是否启用内存答案缓存
         */
        private boolean enabled = true;

        /**
         * 缓存最大条目数
         */
        private long maxSize = 1000;

        /**
         * 缓存过期时间(小时)
         */
        private long expireHours = 24;
    }

}
