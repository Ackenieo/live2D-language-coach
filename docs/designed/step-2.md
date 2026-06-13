# Phase 2 任务计划 — 后端补齐

> 目标：补齐 Phase 1 遗留的后端缺口（提示词模板、对话持久化到 MySQL、User 实体对齐）。
> 状态：后端核心业务逻辑已完成（WebSocket 对话、发音评测、语法纠错、用户认证）。
> 前端：另开项目独立进行。

---

## Phase 1 完成度回顾

| 模块 | 状态 | 说明 |
|------|------|------|
| user 域（认证/用户） | ✅ 代码完成 | AuthController、JWT、SMS 已可用 |
| conversation 域（会话/记忆） | ✅ 代码完成 | ChatSessionService、Redis 记忆、Controller |
| evaluation 域（评测） | ✅ 代码完成 | 腾讯智聆 + 豆包，EvaluationService |
| realtime 域（WS 对话） | ✅ 代码完成 | BailianRealtimeClient、ChatWebSocketHandler |
| 数据库表 | ✅ 完成 | t_user、t_chat_session、t_chat_message 已就绪 |
| 提示词模板 | ✅ 完成 | 7 个场景模板（default/hotel/restaurant/airport/shopping/hospital/business） |
| 对话持久化 | ✅ 完成 | 会话 + 消息异步写入 MySQL |

---

## Step 2 任务列表

### T1: 数据库表补全 ✅

`data/schema.sql` 包含三张表：`t_user`、`t_chat_session`、`t_chat_message`。

### T2: 场景提示词模板 ✅

`resources/prompts/` 下 7 个 `.txt` 模板文件，`PromptTemplateService` 运行时从 classpath 加载。

| 文件 | 场景 | 模板变量 |
|------|------|----------|
| `default.txt` | 通用英语教练 | {difficulty} |
| `hotel.txt` | 酒店入住 | {difficulty}, {guest_name}, {room_type} |
| `restaurant.txt` | 餐厅点餐 | {difficulty}, {cuisine}, {preference} |
| `airport.txt` | 机场出行 | {difficulty}, {destination}, {flight_status} |
| `shopping.txt` | 购物对话 | {difficulty}, {item_category}, {budget} |
| `hospital.txt` | 医院就诊 | {difficulty}, {symptom} |
| `business.txt` | 商务会议 | {difficulty}, {meeting_topic} |

### T3: 对话消息异步入库 ✅

**问题**：对话消息只存 Redis（`ConversationMemoryService` → `RedisChatMemory`），未写 MySQL。

**方案**：通过领域事件 + `@Async` 异步写入，不阻塞对话主流程。

**新增文件**：

| 文件 | 作用 |
|------|------|
| `ChatMessageRepository.java` | 领域接口 |
| `ChatMessageRepositoryImpl.java` | MyBatis-Plus 实现（无需 XML） |
| `ChatMessagePersistenceService.java` | `@Async` + `@EventListener` 监听领域事件 |

**数据流**：

```
用户语音 ──→ ASR 识别 ──→ UserTranscriptCompleteEvent
                                │
                                ├──→ ChatWebSocketHandler（转发前端）
                                └──→ ChatMessagePersistenceService（@Async → t_chat_message）

AI 回复 ──→ 百炼返回 ──→ AiSubtitleCompleteEvent
                                │
                                ├──→ ChatWebSocketHandler（转发前端）
                                └──→ ChatMessagePersistenceService（@Async → t_chat_message）
```

**sessionId 透传**：`ChatSessionService.startSession()` 新增重载支持预先生成的 sessionId，确保 WS 层 UUID 与 DB 记录一致。

**修改文件**：

| 文件 | 改动 |
|------|------|
| `ChatSessionService.java` | 新增 `startSession(userId, scene, difficulty, accent, sessionId)` |
| `ChatWebSocketHandler.java` | 连接时写 `t_chat_session`，关闭时写 `ended_at` |

### T4: User 实体对齐数据表 ✅

数据表 `t_user` 字段：`id, phone, password, nickname, deleted, created_at, updated_at`

`user/domain/entity/User.java` 字段（phone、password、nickname + BaseEntity 的 id/createdAt/updatedAt/deleted）与表字段完全对齐，MyBatis-Plus 驼峰自动映射。

### T5: curl 验证 ⏭️

> 跳过，延期到 Step 3 统一验证。

---

## 任务执行顺序

```
T1 ✅ → T2 ✅ → T3 ✅ → T4 ✅ → T5 ⏭️ (→ Step 3)
```

---

## Step 2 完成标准

- [x] 数据库包含 `t_user`、`t_chat_session`、`t_chat_message` 三张表
- [x] 7 个场景提示词模板文件就绪，`PromptTemplateService` 可加载
- [x] 对话消息通过 `@Async` 异步入库，不阻塞对话主流程
- [x] WebSocket 连接/关闭时自动创建/结束 `t_chat_session` 记录
- [x] `mvn compile` 无编译错误
- [x] User 实体字段对齐数据表
- [ ] curl 测试登录 API 返回 JWT（延至 Step 3）
