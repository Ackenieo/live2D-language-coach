# Phase 3 任务计划 — 剩余后端业务

> 目标：实现设计文档中剩余的后端 API（报告、历史、排行榜、用户资料）。
> Phase 2 已补齐：数据库表、提示词模板、对话异步入库。

---

## 当前完成度 vs 设计文档

| 设计文档 API | 状态 | 说明 |
|-------------|------|------|
| `POST /api/auth/send-sms` | ✅ | AuthController |
| `POST /api/auth/login` | ✅ | AuthController |
| `GET /api/user/profile` | ❌ | 新需求 |
| `PUT /api/user/profile` | ❌ | 新需求 |
| `PUT /api/user/phone` | ❌ | 新需求 |
| `POST /api/chat/start` | ⏭️ | WebSocket 已覆盖，无需 REST |
| `POST /api/chat/message` | ⏭️ | WebSocket 文字消息已支持 |
| `POST /api/chat/voice` | ⏭️ | WebSocket 语音已支持 |
| `POST /api/chat/end` | ⏭️ | WebSocket 关闭时自动结束 |
| `GET /api/chat/report/{sessionId}` | ❌ | 核心新需求 |
| `GET /api/chat/history` | ❌ | 核心新需求 |
| `GET /api/chat/current-score` | ⏭️ | 前端通过 WS 事件实时接收 |
| `GET /api/leaderboard` | ❌ | 新需求 |
| `GET /api/leaderboard/my-rank` | ❌ | 新需求 |

---

## Step 3 任务列表

### T1: User 信息接口

涉及包：`user/`

#### 1.1 GET /api/user/profile
- 返回当前用户信息：id、phone（脱敏 `138****1234`）、nickname
- JWT 鉴权后从 `SecurityContext` 或 token 取 userId

#### 1.2 PUT /api/user/profile
- 修改昵称，2~16 字符校验
- 可扩展：头像 URL（后续接 OSS 上传）

#### 1.3 PUT /api/user/phone
- 步骤：新手机号 → 发验证码 → 输验证码确认 → 更新
- 可拆为两步：`send-sms` 复用，`PUT /api/user/phone` 带验证码确认

新增文件：
- `user/interfaces/rest/ProfileController.java`
- `user/interfaces/dto/UpdateProfileRequest.java`
- `user/interfaces/dto/ChangePhoneRequest.java`

---

### T2: 对话报告接口

涉及包：`conversation/`

#### 2.1 GET /api/chat/report/{sessionId}

聚合 `t_chat_message` 中该 session 的所有评分数据，生成报告：

**聚合逻辑**：
1. 查 `t_chat_session` 得场景/难度/时长
2. 查 `t_chat_message` 中 role=user 的消息，平均 `pron_accuracy` / `pron_fluency` / `pron_completion`
3. 等级转换：`PronunciationResult.fromScore(avg)`
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

#### 2.2 AI 改进建议生成

会话结束时调用 LLM 生成个性化建议：
- 输入：对话摘要（Redis 中的 `ConversationMemoryService.getConversationSummary`）+ 评分数据
- 输出：3~5 条改进建议
- 存入 `t_chat_session` 的备注字段或扩展列

**方案**：`ChatWebSocketHandler.afterConnectionClosed()` → 调 `EvaluationService.generateSuggestions(sessionId)` → `@Async` 调 LLM → 写回 DB。

**实现选择**：本期用豆包 LLM（现有 `DoubaoConfig`），复用 `GrammarCorrectionService` 的 HTTP 调用模式。

---

### T3: 对话历史列表

#### 3.1 GET /api/chat/history

分页返回当前用户的对话列表：

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

**实现**：`ChatSessionMapper` + MyBatis-Plus `LambdaQueryWrapper`，按 `user_id` + 非删除 + `ended_at IS NOT NULL` + `created_at DESC`，分页。

---

### T4: 排行榜接口

涉及包：可放 `conversation/` 或新建 `leaderboard/`

#### 4.1 GET /api/leaderboard

- 按用户综合评分排名
- 支持排序维度：总评 / 准确度 / 流利度 / 完整度
- 分页

**计算逻辑**：
```sql
SELECT u.nickname, 
       AVG(s.total_grade_score) as avg_score,
       COUNT(s.id) as session_count
FROM t_chat_session s 
JOIN t_user u ON s.user_id = u.id
WHERE s.deleted = 0 AND s.ended_at IS NOT NULL
GROUP BY s.user_id
ORDER BY avg_score DESC
```

**响应格式**：
```json
{
  "records": [
    { "rank": 1, "nickname": "Alice", "avgGrade": "A", "sessionCount": 15 },
    { "rank": 2, "nickname": "Bob", "avgGrade": "B+", "sessionCount": 8 }
  ],
  "myRank": { "rank": 12, "avgGrade": "B" }
}
```

**简化方案**：等级 `S/A/B/C/D/E` 映射为 5/4/3/2/1/0 分再排名（同等级按会话数排名）。

#### 4.2 GET /api/leaderboard/my-rank

返回当前用户排名 + 分数。

---

### T5: 会话表评分字段补齐

`t_chat_session` 目前只有 `total_grade`，设计文档要求存 `accuracy_score` / `fluency_score` / `completeness_score` / `overall_score` / `suggestion` / `duration_seconds` / `message_count`。

**实际策略**：不扩表（`t_chat_message` 已有每条消息的评分明细），报告和排行实时聚合计算。仅在 `ChatSession` 结束关闭时更新 `total_grade`（已有）。

---

### T6: curl 验证

```bash
# 1. 用户资料
curl http://localhost:8520/api/user/profile \
  -H "Authorization: Bearer <token>"

# 2. 修改昵称
curl -X PUT http://localhost:8520/api/user/profile \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nickname":"NewName"}'

# 3. 对话报告
curl http://localhost:8520/api/chat/report/<sessionId> \
  -H "Authorization: Bearer <token>"

# 4. 对话历史
curl "http://localhost:8520/api/chat/history?page=1&pageSize=10" \
  -H "Authorization: Bearer <token>"

# 5. 排行榜
curl "http://localhost:8520/api/leaderboard?sort=overall&page=1&pageSize=20" \
  -H "Authorization: Bearer <token>"

# 6. 我的排名
curl http://localhost:8520/api/leaderboard/my-rank \
  -H "Authorization: Bearer <token>"
```

---

## 新增/修改文件清单

```
src/main/java/com/ackenieo/init_pro/
├── user/
│   ├── interfaces/rest/ProfileController.java      ★新增
│   └── interfaces/dto/
│       ├── UpdateProfileRequest.java               ★新增
│       └── ChangePhoneRequest.java                 ★新增
│
├── conversation/
│   ├── domain/service/
│   │   ├── ReportService.java                      ★新增（聚合评分+生成建议）
│   │   └── SuggestionService.java                  ★新增（LLM 生成建议，@Async）
│   ├── interfaces/rest/
│   │   ├── ChatController.java                     ★修改（加 /report/{id}, /history）
│   │   └── LeaderboardController.java              ★新增
│   └── infrastructure/persistence/
│       └── ChatSessionMapper.java                  ★修改（加 findHistoryByUserId, findSessionRankings）
```

---

## 任务执行顺序

```
T1 (User 信息) ──→ T3 (对话历史) ──→ T2 (报告+建议) ──→ T4 (排行榜) ──→ T6 (curl 验证)
                        ↘
                          T5 (评分聚合逻辑贯穿 T2/T4)
```

---

## Step 3 完成标准

- [ ] `GET /api/user/profile` 返回用户信息
- [ ] `PUT /api/user/profile` 可修改昵称
- [ ] `GET /api/chat/report/{sessionId}` 返回聚合评分报告
- [ ] `GET /api/chat/history` 返回分页对话历史
- [ ] `GET /api/leaderboard` 返回排行榜
- [ ] `GET /api/leaderboard/my-rank` 返回我的排名
- [ ] 对话结束时异步生成 AI 改进建议并入库
- [ ] `mvn compile` 无编译错误
- [ ] curl 全部 API 测试通过
