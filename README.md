# 通义千问 AI 题库 API

本项目是基于 Spring Boot 的通义千问 AI 题库服务，打包后生成的 jar 文件可以直接运行，同时提供一个默认配置文件 `application.yml`，用户可以根据自己的 API key 和模型修改配置。

---

## 打包内容

打包完成后，目录中包含：

- `emqo-qwen-answer-api-0.0.1-SNAPSHOT.jar`：程序主 jar 包  
- `application.yml`：默认配置文件，包含示例 API key 和模型

---

## 配置说明

`application.yml` 内容示例：

```yaml
server:
  port: 8080

qwen:
  api:
    key: REPLACE_WITH_YOUR_KEY
    url: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
    model: qwen-flash

spring:
  application:
    name: emqo-qwen-answer-api
```

## 运行方式

在目录下执行：

```
java -jar emqo-qwen-answer-api-0.0.1-SNAPSHOT.jar
```

程序启动后，会监听配置文件中指定的端口（默认 8080），提供题库接口。

## 题库接口配置示例

在OCS脚本中添加以下题库配置：

```
[
  {
    "name": "通义千问AI题库",
    "homepage": "http://localhost:8080",
    "url": "http://localhost:8080/api/search",
    "method": "get",
    "contentType": "json",
    "type": "fetch",
    "data": {
      "title": "${title}",
      "type": "${type}",
      "options": "${options}"
    },
    "handler": "return (res) => res.code === 1 ? [res.data.question, res.data.answer] : [res.message, undefined]"
  }
]
```
