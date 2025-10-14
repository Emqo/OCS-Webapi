package xyz.emqo.qwenxuexitong.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "qwen.api")
public class QwenConfig {

    /**
     * 通义千问 API Key
     */
    private String key;

    /**
     * 通义千问 API 地址
     */
    private String url;

    /**
     * 使用的模型名称
     */
    private String model;

}