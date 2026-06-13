# Phase 4 任务计划 — 剩余后端 API + 数据补齐

> 目标：完成设计文档中所有未实现的后端 API、补齐数据库字段、接入 ASR/OSS 外部服务。
> 前置：Step 1 (DDD 架构迁移)、Step 2 (数据库补齐)、Step 3 (WS 生命周期) 已完成。
> 2026-06-13 创建。

---

## 未实现内容总览

### A. 设计文档 API 缺口（Step 3 遗留）

| 设计文档 API | 状态 | 所在域 |
|-------------|------|--------|
| `GET /api/user/profile` | ❌ | user |
| `PUT /api/user/profile` | ❌ | user |
| `PUT /api/user/phone` | ❌ | user |
| `GET /api/chat/report/{sessionId}` | ❌ | conversation |
| `GET /api/chat/history` | ❌ | conversation |
| `GET /api/leaderboard` | ❌ | conversation |
| `GET /api/leaderboard/my-rank` | ❌ | conversation |

### B. 数据表字段缺口（设计文档 vs 实际）

**t_user** 缺失字段：
- `avatar_url` VARCHAR(255) — 头像 URL
- `total_score` DECIMAL(3,1) — 综合总分（由会话平均分聚合）

**t_chat_session** 缺失字段：
- `accuracy_score` CHAR(2) — 会话平均准确度等级
- `fluency_score` CHAR(2) — 会话平均流利度等级
- `completeness_score` CHAR(2) — 会话平均完整度等级
- `overall_score` CHAR(2) — 会话总分等级（替代现有 `total_grade`）
- `suggestion` TEXT — 会话级 AI 改进建议
- `duration_seconds` INT — 对话时长（秒）
- `message_count` INT — 消息轮数

> 不做逐轮评分结果持久化：智聆/豆包每轮结果只用于实时展示和会话结束时内存聚合，最终只落会话级平均结果。


### C. 外部服务缺口

| 服务 | 说明 | 现状 |
|------|------|------|
| ASR（语音识别） | 用户语音转文字 | ⚠️ 部分（百炼内置 ASR） |
| OSS（对象存储） | 头像上传存储 | ❌ 未接入 |
| AI 改进建议 | 对话结束调用 LLM 生成建议 | ❌ 未实现 |

### D. 前端缺口（独立项目，不在本次范围）

- 欢迎页、对话页、报告页、排行榜页、个人中心页
- Live2D 角色渲染与动画
- 摄像头检测

> **本次 Step 4 范围：仅后端**。前端另开项目独立进行。

---

## Step 4 任务列表

### T1: User 信息接口 ✅（Step 3 遗留）

涉及包：`user/`

#### 1.1 GET /api/user/profile
- JWT 鉴权，从 SecurityContext 取 userId
- 返回：`id`、`phone`（脱敏 `138****1234`）、`nickname`、`avatarUrl`

#### 1.2 PUT /api/user/profile
- 修改昵称（2~16 字符校验）
- 可扩展头像 URL（T4 OSS 上传后更新）

#### 1.3 PUT /api/user/phone
- Body: `{ "newPhone": "13900000001", "code": "123456" }`
- 先调 `POST /api/auth/send-sms` 发验证码到新手机号
- 带验证码调此接口确认换绑

新增文件：
- `user/interfaces/rest/ProfileController.java`
- `user/interfaces/dto/UpdateProfileRequest.java`
- `user/interfaces/dto/ChangePhoneRequest.java`

---

### T2: 数据库字段补齐

#### 2.1 t_user 追加字段
```sql
ALTER TABLE t_user
  ADD COLUMN avatar_url VARCHAR(255) DEFAULT NULL COMMENT '头像URL' AFTER nickname,
  ADD COLUMN total_score DECIMAL(3,1) DEFAULT 0.0 COMMENT '综合总分' AFTER avatar_url;
```

#### 2.2 t_chat_session 追加字段
```sql
ALTER TABLE t_chat_session
  ADD COLUMN accuracy_score CHAR(2) DEFAULT NULL COMMENT '准确度等级' AFTER total_grade,
  ADD COLUMN fluency_score CHAR(2) DEFAULT NULL COMMENT '流利度等级' AFTER accuracy_score,
  ADD COLUMN completeness_score CHAR(2) DEFAULT NULL COMMENT '完整度等级' AFTER fluency_score,
  ADD COLUMN overall_score CHAR(2) DEFAULT NULL COMMENT '总分等级' AFTER completeness_score,
  ADD COLUMN suggestion TEXT DEFAULT NULL COMMENT 'AI改进建议' AFTER overall_score,
  ADD COLUMN duration_seconds INT DEFAULT 0 COMMENT '对话时长(秒)' AFTER suggestion,
  ADD COLUMN message_count INT DEFAULT 0 COMMENT '消息轮数' AFTER duration_seconds;
```

#### 2.3 实体类对齐
- `User.java` 加 `avatarUrl`、`totalScore` 字段
- `ChatSession.java` 加评分明细、duration/messageCount 字段

---

### T3: 对话报告接口（Step 3 T2/T3 合并）

涉及包：`conversation/`

#### 3.1 GET /api/chat/report/{sessionId}

聚合 `t_chat_message` 中该 session 的所有评分数据：

**聚合逻辑**：
1. 查 `t_chat_session` 得场景/难度/时长
2. 查 `t_chat_message` 中 role=user 的消息，**遍历**计算各维度平均分
3. 等级转换 `PronunciationResult.fromScore(avg)`
4. 查消息总数 = 对话轮数

**响应格式**：
```json
{
  "sessionId": "xxx",
  "scene": "hotel",
  "difficulty": "easy",
  "durationSeconds": 204,
  "messageCount": 12,
  "overallGrade": "B",
  "accuracyGrade": "C+",
  "fluencyGrade": "B",
  "completenessGrade": "C",
  "suggestions": [
    "注意 /θ/ 和 /ð/ 的发音区别",
    "尝试使用更丰富的词汇"
  ],
  "createdAt": "2026-06-13T10:30:00"
}
```

**实现**：新增 `conversation/domain/service/ReportService.java`，`ChatController` 加 `GET /report/{sessionId}` 端点。

#### 3.2 GET /api/chat/history

分页返回当前用户的对话列表（仅已结束的会话）：

```
GET /api/chat/history?page=1&pageSize=20
```

**响应格式**：
```json
{
  "records": [
    {
      "sessionId": "xxx",
      "scene": "hotel",
      "difficulty": "easy",
      "totalGrade": "C+",
      "durationSeconds": 204,
      "messageCount": 12,
      "createdAt": "2026-06-13T10:30:00"
    }
  ],
  "total": 25,
  "page": 1,
  "pageSize": 20
}
```

**实现**：`ChatSessionMapper` 加 `selectHistoryByUserId(userId, page, pageSize)`，`ChatController` 加 `GET /history` 端点。

---

### T4: 排行榜接口（Step 3 T4）

涉及包：`conversation/`（无需新建 leaderboard 包，用 conversation 域即可）

#### 4.1 GET /api/leaderboard

按综合评分排名，支持排序维度切换：

```
GET /api/leaderboard?sort=overall&page=1&pageSize=20
```

sort 可选值：`overall` / `accuracy` / `fluency` / `completeness`

**聚合 SQL**：
```sql
SELECT s.user_id, u.nickname,
       AVG(CASE WHEN sort_dim = 'overall' THEN overall_score_num
                WHEN sort_dim = 'accuracy' THEN accuracy_score_num
                WHEN sort_dim = 'fluency' THEN fluency_score_num
                WHEN sort_dim = 'completeness' THEN completeness_score_num END) as avg_score,
       COUNT(s.id) as session_count
FROM t_chat_session s
JOIN t_user u ON s.user_id = u.id
WHERE s.deleted = 0 AND s.ended_at IS NOT NULL
GROUP BY s.user_id
ORDER BY avg_score DESC
```

**简化方案**：MyBatis-Plus `LambdaQueryWrapper` 查所有已结束会话，**纯 Java 内存聚合**（用户量不大时足够）。等级 `S/A/B/C/D/E` 映射为 5/4/3/2/1/0 分再加权。

**响应格式**：
```json
{
  "records": [
    { "rank": 1, "nickname": "Alice", "avgGrade": "A", "sessionCount": 15 },
    { "rank": 2, "nickname": "Bob", "avgGrade": "B+", "sessionCount": 8 }
  ],
  "myRank": { "rank": 12, "avgGrade": "B", "sessionCount": 5 }
}
```

#### 4.2 GET /api/leaderboard/my-rank

返回当前用户排名 + 分数，复用 T4.1 的聚合结果。

**实现**：新增 `conversation/interfaces/rest/LeaderboardController.java` + `conversation/domain/service/LeaderboardService.java`。

---

### T5: AI 改进建议生成（对话结束时触发）

涉及包：`conversation/` + `evaluation/`

#### 5.1 触发时机
`ChatWebSocketHandler.afterConnectionClosed()` → 调 `SuggestionService.generateSuggestions(sessionId)` → `@Async` 调 LLM → 写回 `t_chat_session.suggestion`。

#### 5.2 LLM 调用
- 输入：对话摘要（`ConversationMemoryService.getConversationSummary`）+ 评分数据
- 输出：3~5 条改进建议（JSON 数组）
- 复用豆包 HTTP API（`DoubaoConfig`），同 `GrammarCorrectionService` 的调用模式
- 写入 `t_chat_session.suggestion` + `overall_score`

#### 5.3 文件
新增：
- `conversation/domain/service/SuggestionService.java` — `@Async` + 调 LLM + 写回 DB
- `conversation/infrastructure/external/LlmClient.java` — 通用 LLM HTTP 调用（豆包/百炼）

---

### T6: OSS 头像上传

涉及包：`user/` + 新建 `oss/` 域

#### 6.1 接口抽象
```java
// oss/OssClient.java
public interface OssClient {
    String upload(byte[] data, String fileName);
}
```

#### 6.2 实现
- 接入阿里云 OSS / MinIO（本地开发用 MinIO，生产用阿里云 OSS）
- `POST /api/user/avatar` — 上传头像图片（multipart/form-data）
- 返回 `avatarUrl`，写入 `t_user.avatar_url`

#### 6.3 MinIO 本地开发方案
```yaml
# docker-compose.yml 追加
minio:
  image: minio/minio
  ports:
    - "9000:9000"
    - "9001:9001"
  command: server /data --console-address ":9001"
```

---

### T7: chat_session 结束写入补齐

`ChatWebSocketHandler` 结束后，将内存中的每轮智聆/豆包结果汇总为会话级平均值并写入 `t_chat_session`。不持久化逐轮评分明细。

| 字段 | 来源 |
|------|------|
| `ended_at` | `new Date()` |
| `duration_seconds` | `(now - session.createdAt) / 1000` |
| `message_count` | `COUNT(t_chat_message WHERE session_id = ?)` |
| `accuracy_score` | AVG 发音得分 → 等级 |
| `fluency_score` | AVG 流利度 → 等级 |
| `completeness_score` | AVG 完整度 → 等级 |
| `overall_score` | 三维度均值 → 等级 |

**实现**：`ChatWebSocketHandler.finishSession()` 或 `ChatSessionService.endSession()` 中计算并写入。

---

## 新增/修改文件清单

```
src/main/java/com/ackenieo/init_pro/
├── user/
│   ├── domain/entity/User.java                      ★修改（加 avatarUrl, totalScore）
│   ├── interfaces/rest/ProfileController.java        ★新增
│   └── interfaces/dto/
│       ├── UpdateProfileRequest.java                  ★新增
│       └── ChangePhoneRequest.java                    ★新增
│
├── conversation/
│   ├── domain/
│   │   ├── entity/ChatSession.java                   ★修改（加评分/times/count 字段）
│   │   ├── entity/ChatMessage.java                   ★修改
│   │   ├── service/
│   │   │   ├── ReportService.java                    ★新增
│   │   │   ├── LeaderboardService.java               ★新增
│   │   │   └── SuggestionService.java                ★新增
│   ├── infrastructure/
│   │   ├── external/
│   │   │   └── LlmClient.java                        ★新增
│   │   ├── config/LlmConfig.java                    ★新增
│   │   └── persistence/
│   │       ├── ChatSessionMapper.java                ★修改（加 history/leaderboard 查询）
│   │       └── ChatMessageMapper.java                ★修改（加聚合查询）
│   └── interfaces/rest/
│       ├── ChatController.java                       ★修改（加 /report, /history）
│       └── LeaderboardController.java                ★新增
│
├── oss/
│   ├── OssClient.java                                ★新增（接口）
│   ├── MinioOssClient.java                           ★新增（实现）
│   └── OssConfig.java                                ★新增
│
└── InitProApplication.java                            （不变）

data/
└── schema.sql                                        ★修改（ALTER TABLE 追加）
```

---

## 任务执行顺序

```
T2 (字段补齐) ──→ T1 (User 信息) ──→ T3 (报告+历史) ──→ T7 (结束写入) ──→ T5 (AI 建议)
                                                                              │
                        T4 (排行榜) ←──────────────────────────────────────────┘
                        
                        T6 (OSS) ←── 独立任务，不依赖其他
```

---

## curl 验证清单

```bash
# 前置：获取 token
TOKEN=$(curl -s -X POST http://localhost:8520/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000001","code":"123456"}' | jq -r '.data.token')

# T1.1 获取用户信息
curl http://localhost:8520/api/user/profile \
  -H "Authorization: Bearer $TOKEN"

# T1.2 修改昵称
curl -X PUT http://localhost:8520/api/user/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"NewName"}'

# T1.3 换绑手机号（先发验证码）
curl -X POST http://localhost:8520/api/auth/send-sms \
  -H "Content-Type: application/json" \
  -d '{"phone":"13900000001"}'

curl -X PUT http://localhost:8520/api/user/phone \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newPhone":"13900000001","code":"123456"}'

# T3.1 对话报告
curl http://localhost:8520/api/chat/report/<sessionId> \
  -H "Authorization: Bearer $TOKEN"

# T3.2 对话历史
curl "http://localhost:8520/api/chat/history?page=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN"

# T4.1 排行榜
curl "http://localhost:8520/api/leaderboard?sort=overall&page=1&pageSize=20" \
  -H "Authorization: Bearer $TOKEN"

# T4.2 我的排名
curl http://localhost:8520/api/leaderboard/my-rank \
  -H "Authorization: Bearer $TOKEN"

# T6 上传头像
curl -X POST http://localhost:8520/api/user/avatar \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@avatar.png"
```

---

## Step 4 完成标准

- [ ] `GET /api/user/profile` 返回用户信息（含手机号脱敏）
- [ ] `PUT /api/user/profile` 可修改昵称
- [ ] `PUT /api/user/phone` 可换绑手机号
- [ ] `GET /api/chat/report/{sessionId}` 返回聚合评分报告
- [ ] `GET /api/chat/history` 返回分页对话历史
- [ ] `GET /api/leaderboard` 返回排行榜（支持排序切换）
- [ ] `GET /api/leaderboard/my-rank` 返回当前用户排名
- [ ] 对话结束时 `t_chat_session` 写入会话级平均分/时长/轮数
- [ ] 对话结束时 AI 异步生成改进建议并入库
- [ ] OSS 头像上传 + avatar_url 写入 t_user
- [ ] 数据表字段与设计文档对齐
- [ ] `mvn compile` 无编译错误
- [ ] curl 全部 API 测试通过
