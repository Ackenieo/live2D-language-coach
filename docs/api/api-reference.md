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

```http
Authorization: Bearer <accessToken>
```

Token 通过 `/api/auth/login` 获取，过期后通过 `/api/auth/refresh` 刷新。

### 测试联调建议

开发联调时可在 `dev` 环境下使用：

```http
POST /api/test/generate-token?userId=test-user-1&phone=13800138000
```

前提是数据库中存在对应 `userId` 的用户记录；否则某些需要查当前用户资料的接口会返回“用户不存在”。

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
    "refreshToken": "eyJhbG...",
    "userId": "uuid-user-id",
    "phone": "13800138000"
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
    "refreshToken": "eyJhbG...",
    "userId": "uuid-user-id",
    "phone": "13800138000"
  }
}
```

---

### 1.2 用户模块 — `/api/user`

以下接口**需要 Authorization 头**。

#### GET `/api/user/profile` — 获取当前用户资料

返回当前登录用户资料。

**响应示例：**

```json
{
  "success": true,
  "data": {
    "id": "test-user-1",
    "phone": "138****8000",
    "nickname": "NewName",
    "avatarUrl": "https://..."
  },
  "message": null
}
```

说明：
- `phone` 为脱敏展示
- `avatarUrl` 可能为空

---

#### PUT `/api/user/profile` — 修改当前用户昵称

| 项 | 值 |
|---|-----|
| Content-Type | `application/json` |

**请求体：**

```json
{ "nickname": "NewName" }
```

**约束：**
- 昵称长度必须为 `2 ~ 16` 个字符

**响应示例：**

```json
{
  "success": true,
  "data": {
    "id": "test-user-1",
    "phone": "138****8000",
    "nickname": "NewName",
    "avatarUrl": null
  },
  "message": null
}
```

---

#### PUT `/api/user/phone` — 换绑手机号

| 项 | 值 |
|---|-----|
| Content-Type | `application/json` |

**请求体：**

```json
{
  "newPhone": "13900000001",
  "code": "123456"
}
```

说明：
- 需先通过 `/api/auth/sms/send` 给新手机号发送验证码
- 新手机号不能和当前手机号相同
- 新手机号不能已被其他用户绑定

**响应示例：**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "userId": "test-user-1",
    "phone": "13900000001"
  },
  "message": null
}
```

说明：
- 换绑成功后会重新签发一组 token

---

#### POST `/api/user/avatar` — 上传头像

| 项 | 值 |
|---|-----|
| Content-Type | `multipart/form-data` |

**表单字段：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | 文件 | 是 | 图片文件 |

**约束：**
- 仅支持 `image/*`
- 文件大小不超过 `5MB`

**响应示例：**

```json
{
  "success": true,
  "data": {
    "avatarUrl": "http://english-ai-coach.oss-cn-beijing.aliyuncs.com/avatar/test-user-1/xxx.png?Expires=..."
  },
  "message": null
}
```

说明：
- 当前返回的是 OSS 签名 URL
- 签名 URL 有有效期，前端应视为可直接展示资源地址

---

### 1.3 对话模块 — `/api/chat`

以下接口**需要 Authorization 头**。

#### GET `/api/chat/sessions/{sessionId}/history` — 查询 Redis 对话历史

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

#### DELETE `/api/chat/sessions/{sessionId}/history` — 清空 Redis 对话历史

清除 Redis 中该会话的记忆。

**响应：**

```json
{ "success": true, "data": null }
```

---

#### GET `/api/chat/sessions/{sessionId}/summary` — 获取 Redis 对话摘要

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

#### GET `/api/chat/report/{sessionId}` — 查询会话报告

返回指定会话的聚合评分报告。

说明：
- 只允许查询当前登录用户自己的 session
- 评分基于 `t_chat_message` 中 `role=user` 的评分字段聚合
- `suggestions` 基于 `t_chat_session.suggestion` 拆分返回

**响应示例：**

```json
{
  "success": true,
  "data": {
    "sessionId": "test-session-1",
    "scene": "hotel",
    "difficulty": "easy",
    "accent": "us",
    "durationSeconds": 204,
    "messageCount": 4,
    "overallGrade": "B",
    "accuracyGrade": "B",
    "fluencyGrade": "C",
    "completenessGrade": "B",
    "suggestions": [
      "注意时态一致性",
      "回答时尽量更完整"
    ],
    "createdAt": "2026-06-13T10:27:50"
  },
  "message": null
}
```

---

#### GET `/api/chat/history?page=1&pageSize=20` — 查询当前用户会话历史

返回当前登录用户的**已结束会话**分页列表。

**查询参数：**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| page | `1` | 页码，从 1 开始 |
| pageSize | `20` | 每页条数 |

**响应示例：**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "sessionId": "test-session-1",
        "scene": "hotel",
        "difficulty": "easy",
        "overallGrade": "B",
        "durationSeconds": 204,
        "messageCount": 4,
        "createdAt": "2026-06-13T10:27:50"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  },
  "message": null
}
```

---

### 1.4 排行榜模块 — `/api/leaderboard`

以下接口**需要 Authorization 头**。

#### GET `/api/leaderboard?page=1&pageSize=20` — 查询排行榜

基于**已结束会话**聚合排行榜。

当前实现说明：
- 从 `t_chat_session` 中取已结束会话
- 按 `overallScore` 做等级映射后聚合平均分
- 当前返回字段已满足前端展示需求

**查询参数：**

| 参数 | 默认值 | 说明 |
|------|--------|------|
| page | `1` | 页码 |
| pageSize | `20` | 每页条数 |

**响应示例：**

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "rank": 1,
        "userId": "test-user-1",
        "nickname": "NewName",
        "avatarUrl": "http://english-ai-coach.oss-cn-beijing.aliyuncs.com/avatar/test-user-1/xxx.png?Expires=...",
        "avgGrade": "B",
        "sessionCount": 1
      }
    ],
    "myRank": {
      "rank": 1,
      "userId": "test-user-1",
      "nickname": "NewName",
      "avatarUrl": "http://english-ai-coach.oss-cn-beijing.aliyuncs.com/avatar/test-user-1/xxx.png?Expires=...",
      "avgGrade": "B",
      "sessionCount": 1
    }
  },
  "message": null
}
```

---

#### GET `/api/leaderboard/my-rank` — 查询当前用户排名

返回当前用户在排行榜中的排名信息。

**响应示例：**

```json
{
  "success": true,
  "data": {
    "rank": 1,
    "userId": "test-user-1",
    "nickname": "NewName",
    "avatarUrl": "http://english-ai-coach.oss-cn-beijing.aliyuncs.com/avatar/test-user-1/xxx.png?Expires=...",
    "avgGrade": "B",
    "sessionCount": 1
  },
  "message": null
}
```

---

### 1.5 测试接口 — `/api/test`（仅 dev 环境）

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
curl -X POST "http://localhost:8520/api/test/generate-token?userId=test-user-1&phone=13800138000"
```

---

## 2. WebSocket 接口

### 2.1 连接

```text
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
| `finish` | 结束通话，触发 `session_end` 报告与会话聚合写回 | `{"type":"finish"}` |
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
| `session_end` | 对话结束，含完整报告与建议 | 见下方 |
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
  "turnCount": 4,
  "overallGrade": "B",
  "accuracyGrade": "B",
  "fluencyGrade": "C",
  "completionGrade": "B",
  "summary": "本次对话 03:24，共 2 轮，总评 B。",
  "suggestions": [
    "注意时态一致性",
    "回答时尽量更完整"
  ]
}
```

说明：
- `turnCount` 当前实现更接近“消息数/轮次统计结果”，前端展示时可直接按次数展示
- 结束后会触发会话聚合写回，供 report/history/leaderboard 使用

---

## 3. 前端接入建议

### 最小接入顺序
1. 通过 `/api/auth/login` 或 `/api/test/generate-token` 获取 token
2. 调 `/api/user/profile` 获取用户信息
3. 用 token 连接 WebSocket
4. 会话结束后：
   - 可先用 `session_end` 做即时展示
   - 再调用 `/api/chat/report/{sessionId}` 获取稳定报告
5. 个人中心可调用：
   - `/api/chat/history`
   - `/api/leaderboard`
   - `/api/leaderboard/my-rank`

### 当前已完成的 Step 4 REST 接口

| 接口 | 状态 |
|------|------|
| `GET /api/user/profile` | ✅ |
| `PUT /api/user/profile` | ✅ |
| `PUT /api/user/phone` | ✅ |
| `POST /api/user/avatar` | ✅ |
| `GET /api/chat/report/{sessionId}` | ✅ |
| `GET /api/chat/history` | ✅ |
| `GET /api/leaderboard` | ✅ |
| `GET /api/leaderboard/my-rank` | ✅ |

### 当前注意事项
- OSS 上传当前联调已通过，但本地 dev 环境存在配置来源覆盖问题；提交前需恢复为纯配置驱动模式。
- `/api/user/phone` 依赖短信验证码，联调前需确认短信可达或准备测试验证码方案。
- `report/history/leaderboard` 依赖数据库中存在已结束 session 数据。
