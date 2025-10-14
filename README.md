# 通义千问 AI 题库 API

本项目是一个基于 Spring Boot 的通义千问 AI 题库服务，可以将题目发送到通义千问 API 获取答案，并通过 HTTP 接口返回。

打包后生成的 jar 文件可以直接运行，同时会输出一个示例配置文件 `application-example.yml`，用户可以根据自己需求修改配置。
---

## 题库接口配置

你可以在前端或其他调用端添加题库配置，例如：

```json
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
