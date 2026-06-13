# API 接口说明

> 最后更新：2026-06-13
> 端口：`8520`（可通过 `application.yml` 修改）

---

## 通用规范

### 统一响应格式

所有 REST 接口统一使用：

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

### 鉴权

需要鉴权的接口在请求头携带：

```
Authorization: Bearer <accessToken>
```

Token 通过 `/api/auth/login` 获取，过期后通过 `/api/auth/refresh` 刷新。

---

## 1. REST 接口

### 1.1 认证模块 — `/api/auth`

#### POST `/api/auth/sms/send` — 发送短信验证码

无需鉴权。

| 项 | 值 |
|---|-----|
| Content-Type | `application/json` |

**请求体：**

```json
{ "phone": "13800138000" }
```

**响应：**

```json
{ "success": true, "data": null }
```

---

#### POST `/api/auth/login` — 验证码登录

无需鉴权。首次登录自动创建用户。

| 项 | 值 |
|---|-----|
| Content-Type | `application/json` |

**请求体：**

```json
{ "phone": "13800138000", "code": "123456" }
```

**响应：**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG..."
  }
}
```

`accessToken` 有效期 1 小时，`refreshToken` 有效期 7 天。

---

#### POST `/api/auth/refresh` — 刷新 Token

无需鉴权。

| 项 | 值 |
|---|-----|
| Content-Type | `application/json` |

**请求体：**

```json
{ "refreshToken": "eyJhbG..." }
```

**响应：**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG..."
  }
}
```

---

### 1.2 对话模块 — `/api/chat`

以下接口**需要 Authorization 头**。

#### GET `/api/chat/sessions/{sessionId}/history` — 查询对话历史

返回 Redis 中该会话的对话记录。

**响应：**

```json
{
  "success": true,
  "data": {
    "sessionId": "uuid-xxx",
    "messages": [
      { "role": "user", "content": "Hello" },
      { "role": "assistant", "content": "Hi there!" }
    ]
  }
}
```

---

#### DELETE `/api/chat/sessions/{sessionId}/history` — 清空对话历史

清除 Redis 中该会话的记忆。

**响应：**

```json
{ "success": true, "data": null }
```

---

#### GET `/api/chat/sessions/{sessionId}/summary` — 获取对话摘要

返回 Redis 中该会话的 LLM 生成摘要。

**响应：**

```json
{
  "success": true,
  "data": {
    "sessionId": "uuid-xxx",
    "summary": "用户练习了酒店入住场景，发音准确度B，流利度A..."
  }
}
```

---

### 1.3 测试接口 — `/api/test`（仅 dev 环境）

#### POST `/api/test/generate-token` — 生成测试 Token

> `@Profile("dev")`，生产环境不可用。用于前端联调。

| 项 | 值 |
|---|-----|
| Content-Type | `application/x-www-form-urlencoded` |

**请求参数：**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| userId | `test-user` | 用户 ID |
| phone | `13800138000` | 手机号 |

**响应：**

```json
{ "accessToken": "eyJhbG...", "userId": "test-user" }
```

**curl 示例：**

```bash
curl -X POST "http://localhost:8520/api/test/generate-token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "userId=web_user&phone=13800138000"
```

---

## 2. WebSocket 接口

### 2.1 连接

```
ws://localhost:8520/ws/bailian?token=<accessToken>
```

| 参数 | 必须 | 说明 |
|------|------|------|
| token | 是 | JWT accessToken |
| reconnectSessionId | 否 | 断线重连时传旧的 sessionId |

### 2.2 客户端 → 服务端（入站）

消息格式均为 JSON 文本，`type` 字段区分：

| type | 说明 | 示例 |
|------|------|------|
| `config` | 更新场景/难度/口音配置 | `{"type":"config","scene":"hotel","difficulty":"medium","accent":"us","lang":"en","role":"English Coach","vision":"off"}` |
| `start` | 开始通话 | `{"type":"start"}` |
| `finish` | 结束通话，触发 session_end 报告 | `{"type":"finish"}` |
| `text` | 文本输入 | `{"type":"text","text":"I'd like to check in"}` |
| `reconnect` | 断线重连，恢复旧会话 | `{"type":"reconnect","sessionId":"uuid-xxx"}` |
| `ping` | 心跳 | `{"type":"ping"}` |
| `screenshot` | 图片输入（视觉模式） | `{"type":"screenshot","image":"data:image/jpeg;base64,...","prompt":"描述图片"}` |

**二进制音频**：int16 PCM，采样率 16000Hz，单声道。通过 WebSocket BinaryMessage 发送。

### 2.3 服务端 → 客户端（出站）

| type | 触发时机 | 示例 |
|------|----------|------|
| `connected` | WS 连接成功（新会话） | `{"type":"connected","sessionId":"uuid-xxx","reconnect":false}` |
| `reconnected` | WS 重连成功（旧会话） | `{"type":"reconnected","sessionId":"uuid-xxx","reconnect":true}` |
| `reconnect_failed` | 重连失败（session 不存在） | `{"type":"reconnect_failed","reason":"session_not_found"}` |
| `config_updated` | 配置变更已落库 | `{"type":"config_updated","scene":"hotel","difficulty":"easy","accent":"us","vision":false}` |
| `ai_subtitle` | AI 回复流式片段 | `{"type":"ai_subtitle","text":"Certainly"}` |
| `ai_subtitle_complete` | AI 回复完成 | `{"type":"ai_subtitle_complete","text":"Certainly! Do you have a reservation?"}` |
| `user_subtitle` | 用户语音 ASR 识别结果 | `{"type":"user_subtitle","turnId":"turn-1","text":"I'd like to check in"}` |
| `pronunciation_score` | 发音测评结果 | 见下方 |
| `grammar_correction` | 语法纠错结果 | `{"type":"grammar_correction","turnId":"turn-1","text":"I would like to check in"}` |
| `session_end` | 对话结束，含完整报告 | 见下方 |
| `pong` | 心跳响应 | `{"type":"pong","sessionId":"uuid-xxx"}` |

**二进制音频**：int16 PCM，采样率 24000Hz。服务端通过 WebSocket BinaryMessage 发送。

#### pronunciation_score 字段

```json
{
  "type": "pronunciation_score",
  "turnId": "turn-1",
  "text": "I'd like to check in",
  "suggestedScore": "78.5",
  "pronAccuracy": "80.2",
  "pronFluency": "75.8",
  "pronCompletion": "79.1",
  "suggestedGrade": "B",
  "accuracyGrade": "B",
  "fluencyGrade": "B",
  "completionGrade": "B"
}
```

等级标准：S(≥95) / A(≥85) / B(≥70) / C(≥55) / D(≥40) / E(<40)

#### session_end 字段

```json
{
  "type": "session_end",
  "sessionId": "uuid-xxx",
  "durationSeconds": 204,
  "turnCount": 12,
  "overallGrade": "B",
  "accuracyGrade": "C+",
  "fluencyGrade": "B",
  "completionGrade": "C",
  "summary": "本次对话 03:24，共 12 轮，总评 B。",
  "suggestions": [
    "I would like to check in",
    "Can I have a reservation"
  ]
}
```

---

## 3. 当前覆盖率 vs 设计文档

| 设计文档 API | 状态 | 说明 |
|-------------|------|------|
| `POST /api/auth/sms/send` | ✅ | 已完成 |
| `POST /api/auth/login` | ✅ | 已完成 |
| `POST /api/auth/refresh` | ✅ | 已完成 |
| `GET /api/chat/sessions/{sessionId}/history` | ✅ | 已完成 |
| `GET /api/chat/sessions/{sessionId}/summary` | ✅ | 已完成 |
| `GET /api/user/profile` | ❌ | 待实现 |
| `PUT /api/user/profile` | ❌ | 待实现 |
| `PUT /api/user/phone` | ❌ | 待实现 |
| `GET /api/chat/report/{sessionId}` | ❌ | 待实现（聚合 DB 评分） |
| `GET /api/chat/history` | ❌ | 待实现（分页列表） |
| `GET /api/leaderboard` | ❌ | 待实现 |
| `GET /api/leaderboard/my-rank` | ❌ | 待实现 |

> WebSocket 对话链路（start/end/message/voice/current-score）均已通过 WS 消息覆盖，无需单独的 REST 接口。
