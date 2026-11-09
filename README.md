# DeepSeek AI 题库 API (OCS-Webapi)

本项目是基于 Spring Boot 的 DeepSeek AI 题库服务，适用于 [OCS网课助手](https://www.ocsjs.com/) 的自定义题库接口。打包后生成的 jar 文件可以直接运行，同时提供一个默认配置文件 `application.yml`，用户可以根据自己的 API key 和模型修改配置。

> GitHub: <https://github.com/Emqo/OCS-Webapi>

服务端通过 DeepSeek 官方 **OpenAI 兼容协议** 调用模型（`POST https://api.deepseek.com/chat/completions`），无需额外 SDK；接口支持 GET/POST 两种调用方式并开放跨域（CORS），OCS 浏览器脚本可直接 web 调用（`type: "fetch"`）。同一题目默认带内存缓存（24小时），重复刷题不重复消耗 API 额度。

---

## 准备工作

1. 到 [DeepSeek 开放平台](https://platform.deepseek.com/) 注册并创建 API Key。

---

## 打包内容

打包完成后，目录中包含：

- `ocs-webapi-0.0.1.jar`：程序主 jar 包
- `application.yml`：默认配置文件，包含示例 API key 和模型

---

## 配置说明

`application.yml` 内容示例：

```yaml
server:
  port: 8080

deepseek:
  api:
    # 在 https://platform.deepseek.com 申请 API Key
    key: REPLACE_WITH_YOUR_KEY
    # DeepSeek 官方 OpenAI 兼容协议地址
    url: https://api.deepseek.com/chat/completions
    # 模型: deepseek-flash(默认, 快且便宜) / deepseek-v4-pro(更强, 更贵更慢)
    model: deepseek-flash
    # 采样温度(thinking 为 true 时无效, 不会发送)
    temperature: 1.0
    # 单次回答最大 token 数
    max-tokens: 1024
    # 深度思考模式: 更准但更慢、更贵
    thinking: false
    # 调用 DeepSeek 的响应超时(秒)
    timeout-seconds: 60
    cache:
      # 内存答案缓存: 同题不重复调用 API
      enabled: true
      max-size: 1000
      expire-hours: 24

spring:
  application:
    name: ocs-webapi
```

> 也可以把 `application.yml` 放在 jar 同目录，修改后重启即可生效，无需重新打包。

## 运行方式

在目录下执行：

```
java -jar ocs-webapi-0.0.1.jar
```

程序启动后，会监听配置文件中指定的端口（默认 8080），提供题库接口。

## 题库接口配置示例

在OCS脚本中添加以下题库配置：

```
[
  {
    "name": "DeepSeek AI题库",
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

也支持 POST 方式（JSON body，字段同上）：

```
[
  {
    "name": "DeepSeek AI题库",
    "homepage": "http://localhost:8080",
    "url": "http://localhost:8080/api/search",
    "method": "post",
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

> 提示：若浏览器提示跨域失败，可在 OCS 题库配置里把 `type` 改为 `"GM_xmlhttpRequest"`（油猴脚本需在头部添加 `@connect localhost`）。

## 题目类型兼容说明

`type` 字段兼容 OCS 的字符串枚举与数字形式：

| type | 题型 | 答案输出约定 |
| --- | --- | --- |
| `single` / `1` | 单选题 | 只输出选项字母，如 `A` |
| `multiple` / `2` | 多选题 | 字母用 `#` 分隔，如 `A#C` |
| `judgement` / `3` | 判断题 | 只输出 `正确` 或 `错误` |
| `completion` / `4` | 填空题 | 输出简短准确的答案本身 |
| 其他/留空 | 其他 | 输出简短准确的答案本身 |

## 接口返回格式

```json
{
  "code": 1,
  "message": "成功",
  "data": {
    "question": "中国梦是什么？",
    "answer": "实现中华民族伟大复兴，本质是国家富强、民族振兴、人民幸福。"
  }
}
```

失败时 `code` 为 `0`，`message` 为错误原因，`data` 为 `null`。

## 其他接口

- `GET /api/health`：健康检查，返回 `OK`。
