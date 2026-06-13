# Phase 1 任务计划 — 将 talktic 实时对话重构入 DDD 业务分包架构

> 目标：把 `D:\WWWWokP\newWWWWWWork\tmp\ragdemo\talktic\src` 的全部功能重构到当前 DDD 项目中，一步到位。
> 架构原则：**使用 V3 DDD Architecture Skill 进行重构**，按业务域（bounded context）分包，域内平铺（entity/service/repo/config 直接放域目录下），不再嵌套 domain/application/infrastructure/interfaces 子包。
> 运行方式：**单进程 Spring Boot**，所有域在同一个 JAR 内，同一个 main() 启动，域之间通过应用服务接口调用。

---

## 业务域划分

| 业务域 | 包名 | 职责 | 后续扩展 |
|--------|------|------|----------|
| **user** | `user` | 用户认证+资料+排行榜+历史得分 | ProfileController, LeaderboardController |
| **coach** | `coach` | 实时对话+评分+报告（核心业务） | ReportController, SceneController |
| **shared** | `shared` | 跨域共享：基础实体、通用配置、全局异常 | — |

> 合并逻辑：auth + profile + leaderboard 都围绕「用户」→ 合入 user；chat + evaluation 是对话流程中紧密耦合的 → 合入 coach。

---

## 目标包结构（V3 DDD Architecture — 域内平铺）

> V3 DDD Architecture 风格：每个 bounded context 是一个顶级包，域内直接放 entity/service/repo/config，
> 不再嵌套 domain/application/infrastructure/interfaces 子包。
> 所有域共享同一个 Spring Boot 进程，同一个 main()。

```
com.ackenieo.init_pro/
│
├── user/                                    ← 用户域 (bounded context)
│   │
│   │  ── 实体 & 仓库 ──
│   ├── User.java                            （已有，迁移）
│   ├── UserRepository.java                  （已有，迁移）
│   ├── UserDomainService.java               （已有，迁移）
│   ├── UserDomainServiceImpl.java           （已有，迁移）
│   ├── UserMapper.java                      （已有，迁移）
│   ├── UserRepositoryImpl.java              （已有，迁移）
│   │
│   │  ── 认证服务 ──
│   ├── AuthService.java                     （已有，迁移）
│   ├── JwtTokenProvider.java                （已有，迁移）
│   ├── SmsService.java                      （已有，迁移）
│   │
│   │  ── 接口 ──
│   ├── AuthController.java                  （已有，迁移）
│   ├── TestTokenController.java             （已有，迁移）
│   ├── LoginRequest.java                    （已有，迁移）
│   ├── SendSmsRequest.java                  （已有，迁移）
│   ├── RefreshTokenRequest.java             （已有，迁移）
│   │
│   │  ── ★后续扩展 ──
│   ├── ProfileController.java               ★后续：用户资料管理
│   ├── LeaderboardController.java           ★后续：综合排行榜
│   └── UserScoreHistoryService.java         ★后续：历史得分
│
├── coach/                                   ← 教练域 (bounded context) — 核心业务
│   │
│   │  ── 实体 & 值对象 ──
│   ├── ChatSession.java                     ★新增（talktic 无此实体，sessionId 散落在各 service 中）
│   ├── ChatMessage.java                     ★新增（talktic 无此实体，对话只存 Redis 不持久化）
│   ├── PronunciationResult.java             ★从 talktic service/PronunciationEvaluationResult.java 重构
│   │
│   │  ── 领域接口（domain 不依赖 infrastructure）──
│   ├── ChatSessionRepository.java           ★新增（talktic 无仓库层）
│   ├── RealtimeChatClient.java              ★新增接口，抽象自 talktic service/BailianWebSocketClient.java
│   ├── ChatMemory.java                      ★新增接口，抽象自 talktic 对 Spring AI ChatMemory 的依赖
│   ├── PronunciationEvaluator.java          ★新增接口，抽象自 talktic service/PronunciationEvaluationService.java
│   ├── GrammarCorrector.java                ★新增接口，抽象自 talktic service/GrammarCorrectionService.java
│   │
│   │  ── 领域服务（只依赖上方接口）──
│   ├── ChatSessionService.java              ★新增（编排对话生命周期）
│   ├── ConversationMemoryService.java       ★从 talktic service/ConversationMemoryService.java 重构，改依赖 ChatMemory 接口
│   ├── EvaluationService.java               ★新增（编排 PronunciationEvaluator + GrammarCorrector）
│   ├── AudioTurnBufferService.java          ★从 talktic service/AudioTurnBufferService.java 直接重构
│   ├── PromptTemplateService.java           ★从 talktic service/PromptTemplateService.java 重构，扩展场景模板
│   │
│   │  ── 基础设施实现（实现领域接口）──
│   ├── ChatSessionRepositoryImpl.java       ★新增
│   ├── ChatSessionMapper.java               ★新增
│   ├── ChatMessageMapper.java               ★新增
│   ├── BailianRealtimeClient.java           ★从 talktic service/BailianWebSocketClient.java 重构，implements RealtimeChatClient
│   ├── RedisChatMemory.java                 ★从 talktic memory/RedisChatMemory.java 重构，implements ChatMemory
│   ├── PronunciationEvaluationService.java  ★从 talktic service/PronunciationEvaluationService.java 重构，implements PronunciationEvaluator
│   ├── GrammarCorrectionService.java        ★从 talktic service/GrammarCorrectionService.java 重构，implements GrammarCorrector
│   │
│   │  ── 配置 & 接入 ──
│   ├── BailianConfig.java                   ★从 talktic config/BailianConfig.java 重构
│   ├── ChatMemoryConfig.java                ★从 talktic config/ChatMemoryConfig.java 重构
│   ├── EvaluationConfig.java                ★新增（talktic 的 Dotenv 配置集中到此）
│   ├── WebSocketConfig.java                 ★从 talktic config/WebSocketConfig.java 重构
│   ├── ChatWebSocketHandler.java            ★从 talktic websocket/BailianWebSocketHandler.java 重构
│   ├── WebSocketAuthInterceptor.java        ★新增（talktic 无鉴权）
│   ├── ChatController.java                  ★新增（替代 talktic controller/MemoryTestController.java）
│   │
│   │  ── ★后续扩展 ──
│   ├── ReportController.java                ★后续：对话报告
│   └── SceneController.java                 ★后续：场景管理
│
├── shared/                                  ← 共享域 (跨域基础设施)
│   ├── BaseEntity.java                      （已有，迁移）
│   ├── BaseValueObject.java                 （已有，迁移）
│   ├── BaseRepository.java                  （已有，迁移）
│   ├── DomainEvent.java                     （已有，迁移）
│   ├── DomainEventPublisher.java            （已有，迁移）
│   ├── BaseCommand.java                     （已有，迁移）
│   ├── BaseQuery.java                       （已有，迁移）
│   ├── UseCaseResult.java                   （已有，迁移）
│   ├── DddArchitectureConfig.java           （已有，迁移）
│   ├── MybatisPlusConfig.java               （已有，迁移）
│   ├── RedissonConfig.java                  （已有，迁移）
│   ├── SecurityConfig.java                  （已有，迁移）
│   ├── SpringDomainEventPublisher.java      （已有，迁移）
│   ├── BaseController.java                  （已有，迁移）
│   ├── ApiResponse.java                     （已有，迁移）
│   └── GlobalExceptionHandler.java          （已有，迁移）
│
└── InitProApplication.java                  （已有，唯一启动类）
```

---

## talktic 源码 → DDD 依据关系

### talktic 原始调用链

```
前端 H5
  │
  └─ WS ──→ websocket/BailianWebSocketHandler（入口编排）
                │
                ├─ new BailianWebSocketClient(session, sessionId, ...)（非 Spring Bean）
                │     ├→ config/BailianConfig（静态常量: API_KEY, WS_URL, VOICE, INSTRUCTIONS）
                │     ├→ service/ConversationMemoryService（依赖 Spring AI ChatMemory 接口）
                │     │     └→ memory/RedisChatMemory（implements org.springframework.ai.chat.memory.ChatMemory）
                │     ├→ service/AudioTurnBufferService（内存 ConcurrentHashMap 缓冲）
                │     ├→ service/PronunciationEvaluationService（Dotenv 读配置，腾讯智聆 SDK）
                │     └→ service/GrammarCorrectionService（Dotenv 读配置，豆包 HTTP API）
                │
                └→ service/PromptTemplateService（classpath:prompts/ 模板加载）
```

### 逐文件依据关系

| DDD 目标文件 | 分类 | talktic 依据 | 依据内容 | 重构变化 |
|---|---|---|---|---|
| **实体 & 值对象** | | | | |
| `ChatSession.java` | 实体 | 无 | talktic 的 sessionId 是 String 散落在各 service 中，无持久化 | ★新增：聚合根，持久化对话会话 |
| `ChatMessage.java` | 实体 | 无 | talktic 对话只存 Redis（ChatMemory），不持久化到 DB | ★新增：持久化对话消息 |
| `PronunciationResult.java` | 值对象 | `service/PronunciationEvaluationResult.java` | record(turnId, sessionId, text, suggestedScore, pronAccuracy, pronFluency, pronCompletion, suggestedGrade, accuracyGrade, fluencyGrade, completionGrade) | 改名，字母等级标准改为 S/A/B/C/D/E |
| **领域接口** | | | | |
| `ChatSessionRepository.java` | 接口 | 无 | talktic 无仓库层 | ★新增：CRUD 接口 |
| `RealtimeChatClient.java` | 接口 | `service/BailianWebSocketClient.java` 的公共方法 | connect(), sendAudio(), sendText(), setInstructions(), disconnect(), isConnected() | ★新增接口：抽象出 connect/sendAudio/sendText/disconnect |
| `ChatMemory.java` | 接口 | `org.springframework.ai.chat.memory.ChatMemory` | add(), get(), clear() | ★新增接口：去掉 Spring AI 依赖，自建轻量接口 |
| `PronunciationEvaluator.java` | 接口 | `service/PronunciationEvaluationService.java` 的公共方法 | evaluatePronunciationAsync(), isEnabled() | ★新增接口：evaluate(turnId, sessionId, audio, refText) |
| `GrammarCorrector.java` | 接口 | `service/GrammarCorrectionService.java` 的公共方法 | correctGrammar(), isEnabled() | ★新增接口：correct(text) |
| **领域服务** | | | | |
| `ChatSessionService.java` | 服务 | 无 | talktic 无对话生命周期管理 | ★新增：编排 start/end/clear 会话 |
| `ConversationMemoryService.java` | 服务 | `service/ConversationMemoryService.java` | saveMessage(), getConversationHistory(), getConversationSummary(), clearConversation(), hasVisualKeywordInLatestUserText() | 依赖改 ChatMemory 接口；去掉 hasVisualKeywordInLatestUserText |
| `EvaluationService.java` | 服务 | `BailianWebSocketClient.java` 内的 triggerPronunciationEvaluation() + triggerGrammarCorrection() | 评分触发逻辑散落在 Client.onMessage() 中 | ★新增：从 Client 中提取，编排 PronunciationEvaluator + GrammarCorrector |
| `AudioTurnBufferService.java` | 服务 | `service/AudioTurnBufferService.java` | startTurn(), appendAudio(), removeAudio(), clearTurn() | 直接重构，逻辑不变 |
| `PromptTemplateService.java` | 服务 | `service/PromptTemplateService.java` | getSystemPrompt(), getSystemPrompt(name, variables), registerTemplate() | 扩展场景模板，默认提示词改为英语教练 |
| **基础设施实现** | | | | |
| `ChatSessionRepositoryImpl.java` | 实现 | 无 | talktic 无仓库层 | ★新增 |
| `ChatSessionMapper.java` | 实现 | 无 | talktic 无 DB 层 | ★新增 |
| `ChatMessageMapper.java` | 实现 | 无 | talktic 无 DB 层 | ★新增 |
| `BailianRealtimeClient.java` | 实现 | `service/BailianWebSocketClient.java` (353行) | extends WebSocketClient，onMessage() 处理百炼事件，触发评分和记忆 | implements RealtimeChatClient；BailianConfig 静态常量→Bean 注入；去掉 sendImage；评分触发改调 EvaluationService |
| `RedisChatMemory.java` | 实现 | `memory/RedisChatMemory.java` (143行) | implements Spring AI ChatMemory；Redis List + 滑动窗口 + TTL | implements 自建 ChatMemory 接口；去掉 Spring AI 依赖 |
| `PronunciationEvaluationService.java` | 实现 | `service/PronunciationEvaluationService.java` (171行) | Dotenv 读配置；腾讯智聆 SDK；toGrade() 等级转换；@Async 异步 | implements PronunciationEvaluator；Dotenv→@Value；toGrade()→PronunciationResult.fromScore() |
| `GrammarCorrectionService.java` | 实现 | `service/GrammarCorrectionService.java` (128行) | Dotenv 读配置；豆包 HTTP API；中文纠错 prompt | implements GrammarCorrector；Dotenv→@Value；prompt 改英文语法纠错 |
| **配置 & 接入** | | | | |
| `BailianConfig.java` | 配置 | `config/BailianConfig.java` | 静态常量 API_KEY, WS_URL, VOICE, INSTRUCTIONS | @ConfigurationProperties(prefix="bailian") + @Data |
| `ChatMemoryConfig.java` | 配置 | `config/ChatMemoryConfig.java` | @Value 注入 maxMessages/ttlHours/keyPrefix；注册 ChatMemory Bean | 改配置前缀 live2d-coach.memory.*；注册自建 ChatMemory |
| `EvaluationConfig.java` | 配置 | `PronunciationEvaluationService` + `GrammarCorrectionService` 的 Dotenv 配置 | Dotenv 读 TENCENT_*, DOUBAO_* | ★新增：集中 @Value 注入；@ConditionalOnProperty 控制启用 |
| `WebSocketConfig.java` | 配置 | `config/WebSocketConfig.java` | 注册 handler 到 /ws/bailian，allowedOrigins("*") | 端点改 /ws/chat；加 WebSocketAuthInterceptor |
| `ChatWebSocketHandler.java` | 接入 | `websocket/BailianWebSocketHandler.java` (162行) | 管理 clientMap；处理 text/binary/config/screenshot/start/finish 消息类型 | 依赖 RealtimeChatClient 接口；去掉 screenshot；config 扩展 scene/difficulty/accent |
| `WebSocketAuthInterceptor.java` | 接入 | 无 | talktic 无鉴权 | ★新增：握手时 JWT 验证 |
| `ChatController.java` | 接入 | `controller/MemoryTestController.java` (55行) | /api/memory/* 测试接口 | ★新增：生产级 REST API，替代测试控制器 |
| **不迁移** | | | | |
| — | — | `controller/MemoryTestController.java` | 测试用 | 不迁移 |
| — | — | `BailianWebSocketClient.sendImage()` | 截图功能 | 不迁移 |
| — | — | `ConversationMemoryService.hasVisualKeywordInLatestUserText()` | 视觉关键词检测 | 不迁移 |

### talktic → DDD 依赖方向变化

```
talktic 原版（依赖方向混乱）：
BailianWebSocketClient（非 Bean）
  ├→ BailianConfig（静态常量）        ← 硬编码依赖
  ├→ ConversationMemoryService       ← 直接依赖实现
  │     └→ Spring AI ChatMemory      ← 外部框架接口
  ├→ PronunciationEvaluationService  ← 直接依赖实现
  └→ GrammarCorrectionService        ← 直接依赖实现

DDD 重构版（依赖倒置，方向向内）：
ChatWebSocketHandler（Spring Bean）
  └→ RealtimeChatClient 接口          ← 只依赖接口
        └→ BailianRealtimeClient      ← 实现类

EvaluationService（领域服务）
  ├→ PronunciationEvaluator 接口      ← 只依赖接口
  │     └→ PronunciationEvaluationService ← 实现类
  └→ GrammarCorrector 接口            ← 只依赖接口
        └→ GrammarCorrectionService   ← 实现类

ConversationMemoryService（领域服务）
  └→ ChatMemory 接口                  ← 只依赖接口（去掉 Spring AI）
        └→ RedisChatMemory            ← 实现类
```

---

## 任务列表（按执行顺序）

### T1: pom.xml 添加依赖

- [ ] 添加 `org.java-websocket:Java-WebSocket:1.5.4`
- [ ] 添加 `com.tencentcloudapi:tencentcloud-speech-sdk-java:1.0.67`
- [ ] `mvn compile` 验证依赖解析

### T2: application-dev.yml 添加配置项

- [ ] 百炼配置
  ```yaml
  bailian:
    api-key: ${BAILIAN_API_KEY:}
    ws-url: wss://dashscope.aliyuncs.com/api-ws/v1/realtime?model=qwen-omni-turbo-realtime
    voice: Chelsie
  ```
- [ ] 对话记忆配置
  ```yaml
  live2d-coach:
    memory:
      max-messages: 20
      ttl-hours: 24
      key-prefix: "live2d-coach:memory:"
  ```
- [ ] 腾讯智聆配置
  ```yaml
  tencent:
    soe:
      enabled: ${TENCENT_SOE_ENABLED:false}
      app-id: ${TENCENT_APPID:}
      secret-id: ${TENCENT_SECRET_ID:}
      secret-key: ${TENCENT_SECRET_KEY:}
  ```
- [ ] 豆包语法纠错配置
  ```yaml
  doubao:
    enabled: ${DOUBAO_ENABLED:false}
    api-key: ${DOUBAO_API_KEY:}
    base-url: https://ark.cn-beijing.volces.com/api/v3
    correction-model: doubao-seed-1-6
    timeout-millis: 30000
  ```

### T3: 使用 V3 DDD Architecture Skill 重组现有代码

将现有平铺结构的代码迁移到 V3 DDD 业务分包结构中。**逻辑不变，只改包路径和 import**。
使用 `Use Skill: V3 DDD Architecture` 指导重构。

- [ ] 创建业务域目录结构：`user/`, `coach/`, `shared/`
- [ ] 迁移 `user` 域（14 个文件）→ `com.ackenieo.init_pro.user.*`
  - `User.java`, `UserRepository.java`, `UserDomainService.java`, `UserDomainServiceImpl.java`
  - `AuthService.java`, `JwtTokenProvider.java`, `SmsService.java`
  - `UserMapper.java`, `UserRepositoryImpl.java`
  - `AuthController.java`, `TestTokenController.java`
  - `LoginRequest.java`, `SendSmsRequest.java`, `RefreshTokenRequest.java`
- [ ] 迁移 `shared` 域（16 个文件）→ `com.ackenieo.init_pro.shared.*`
  - `BaseEntity.java`, `BaseValueObject.java`, `BaseRepository.java`
  - `DomainEvent.java`, `DomainEventPublisher.java`
  - `BaseCommand.java`, `BaseQuery.java`, `UseCaseResult.java`
  - `DddArchitectureConfig.java`, `MybatisPlusConfig.java`, `RedissonConfig.java`, `SecurityConfig.java`
  - `SpringDomainEventPublisher.java`
  - `BaseController.java`, `ApiResponse.java`, `GlobalExceptionHandler.java`
- [ ] 删除旧平铺目录下的文件
- [ ] 更新所有 import 语句
- [ ] `mvn compile` 验证迁移无编译错误

### T4: coach 域 — 评分值对象 + 配置

- [ ] 创建 `coach/PronunciationResult.java`
  - 从 talktic 的 `PronunciationEvaluationResult.java` 重构
  - record，包名 `com.ackenieo.init_pro.coach`
  - 字母等级转换 `fromScore(Double)` 静态工厂方法
  - 等级标准：S(95+) / A(85+) / B(70+) / C(55+) / D(40+) / E(<40)

- [ ] 创建 `coach/EvaluationConfig.java`
  - 腾讯智聆 + 豆包配置的 @Value 注入点
  - `@ConditionalOnProperty` 控制启用/禁用

### T5: coach 域 — 配置类

- [ ] 创建 `coach/BailianConfig.java`
  - 从 talktic 的 `BailianConfig.java` 重构
  - `@ConfigurationProperties(prefix = "bailian")` + `@Data`
  - 提供 `getApiKey()`, `getWsUrl()`, `getVoice()`

- [ ] 创建 `coach/ChatMemoryConfig.java`
  - 从 talktic 的 `ChatMemoryConfig.java` 重构
  - `@Value` 从 `live2d-coach.memory.*` 读取
  - 注册 `ChatMemory` Bean

- [ ] 创建 `coach/WebSocketConfig.java`
  - 从 talktic 的 `WebSocketConfig.java` 重构
  - 端点 `/ws/chat`，注入 `ChatWebSocketHandler` + `WebSocketAuthInterceptor`

### T6: coach 域 — RedisChatMemory

- [ ] 创建 `coach/RedisChatMemory.java`
  - 从 talktic 的 `memory/RedisChatMemory.java` 重构
  - 改包名 `com.ackenieo.init_pro.coach`
  - 逻辑不变：Redis List + 滑动窗口 + TTL

### T7: coach 域 — 应用服务 + 提示词模板

- [ ] 创建 `coach/ConversationMemoryService.java`
  - 从 talktic 的 `ConversationMemoryService.java` 重构
  - 去掉 `hasVisualKeywordInLatestUserText`
  - 保留 `saveMessage`, `getConversationHistory`, `getConversationSummary`, `clearConversation`

- [ ] 创建 `coach/AudioTurnBufferService.java`
  - 从 talktic 的 `AudioTurnBufferService.java` 直接重构
  - 改包名，逻辑完全不变

- [ ] 创建 `coach/PromptTemplateService.java`
  - 从 talktic 的 `PromptTemplateService.java` 直接重构
  - 默认提示词改为英语口语教练场景

- [ ] 创建 `resources/prompts/` 目录 + 场景模板文件
  - `default.txt` — 默认英语教练
  - `hotel.txt` — 酒店入住场景
  - `restaurant.txt` — 餐厅点餐场景
  - `airport.txt` — 机场出行场景
  - `shopping.txt` — 购物对话场景
  - `hospital.txt` — 医院就诊场景
  - `business.txt` — 商务会议场景

### T8: coach 域 — 百炼 Client

- [ ] 创建 `coach/BailianRealtimeClient.java`
  - 从 talktic 的 `BailianWebSocketClient.java` 重构
  - 构造函数：接收 `BailianConfig` Bean 而非硬编码
  - `BailianConfig.API_KEY` → `bailianConfig.getApiKey()`
  - `BailianConfig.WS_URL` → `bailianConfig.getWsUrl()`
  - `BailianConfig.VOICE` → `bailianConfig.getVoice()`
  - 保留全部事件处理逻辑
  - 移除 `sendImage` 方法
  - `buildInstructions` 改为调用 `PromptTemplateService`
  - 评分触发改为调用同域的 `EvaluationService`

### T9: coach 域 — 发音测评 + 语法纠错

- [ ] 创建 `coach/PronunciationEvaluationService.java`
  - 从 talktic 的 `PronunciationEvaluationService.java` 重构
  - 去掉 `Dotenv`，改用 `@Value` 注入
  - 字母等级转换改为调用 `PronunciationResult.fromScore()`
  - 保留异步评分逻辑 `evaluatePronunciationAsync`

- [ ] 创建 `coach/GrammarCorrectionService.java`
  - 从 talktic 的 `GrammarCorrectionService.java` 重构
  - 去掉 `Dotenv`，改用 `@Value` 注入
  - 纠错 prompt 改为英文语法纠错

- [ ] 创建 `coach/EvaluationService.java`
  - 编排发音测评 + 语法纠错的调用
  - `evaluateTurn(turnId, sessionId, audioData, refText)` → 异步调用两个外部服务
  - 结果汇总后回调通知 WebSocket Handler

### T10: coach 域 — WebSocket Handler + 鉴权

- [ ] 创建 `coach/ChatWebSocketHandler.java`
  - 从 talktic 的 `BailianWebSocketHandler.java` 重构
  - `handleTextMessage` 中 `config` 类型扩展：
    ```json
    {"type":"config","scene":"hotel","difficulty":"easy","accent":"us"}
    ```
  - 去掉 `screenshot` 类型处理
  - `buildInstructions` 改为调用 `PromptTemplateService.getSystemPrompt(scene, variables)`
  - 评分触发委托给 `EvaluationService`

- [ ] 创建 `coach/WebSocketAuthInterceptor.java`
  - 握手时从 `?token=xxx` 取 JWT 验证
  - 验证失败拒绝连接

### T11: 编译验证 + curl 测试

- [ ] `mvn compile` 确认无编译错误
- [ ] 启动 Docker（MySQL + Redis）
- [ ] 启动 Spring Boot
- [ ] curl 测试：
  - `POST /api/auth/send-sms` — 登录接口仍可用
  - `POST /api/auth/login` — 获取 token
  - 用 wscat 连接 `ws://localhost:8520/ws/chat?token=xxx` 验证 WS 握手

### T12: 前端项目初始化

- [ ] `npm create vite@latest frontend -- --template vue-ts`
- [ ] 安装依赖：`vue-router`, `pinia`, `axios`, `tailwindcss`
- [ ] 配置 Tailwind 黑灰主题色板
- [ ] 创建路由骨架：`/login`, `/welcome`, `/chat`, `/report/:id`, `/leaderboard`, `/profile`
- [ ] 路由守卫：未登录跳 `/login`
- [ ] 底部导航组件
- [ ] 全局样式

### T13: 前端登录页

- [ ] `LoginPage.vue`：手机号 + 验证码 + 60s 倒计时
- [ ] 对接 `POST /api/auth/send-sms` 和 `POST /api/auth/login`
- [ ] JWT 存 localStorage → 跳转 `/welcome`

### T14: 前端欢迎页

- [ ] `WelcomePage.vue`：场景卡片 + 难度 + 口音选择
- [ ] 「开始对话」→ 携参数跳 `/chat`

### T15: 前端对话页

- [ ] `ChatPage.vue`：Live2D 占位 + 毛玻璃气泡 + 计时器 + 摄像头开关 + 得分
- [ ] WebSocket 连接 + PCM 录音 + BinaryMessage 发送
- [ ] 接收 AI 音频播放 + 字幕显示 + 评分展示

### T16: 前端报告页

- [ ] `ReportPage.vue`：总评分 + 三维评分 + AI 建议

### T17: 端到端联调

- [ ] 完整流程：登录 → 选场景 → 语音对话 → 评分 → 报告
- [ ] curl 测试后端 API
- [ ] 浏览器测试前端交互

---

## 重构要点总结

### 1. V3 DDD Architecture — 域内平铺，单进程

```
传统 DDD 嵌套分层：              V3 DDD Architecture 域内平铺：
domain/model/entity/User.java   user/User.java
domain/repository/              user/UserRepository.java
application/service/            user/AuthService.java
infrastructure/external/        user/SmsService.java
interfaces/rest/                user/AuthController.java

                                coach/ChatSession.java
                                coach/BailianRealtimeClient.java
                                coach/ChatWebSocketHandler.java
                                coach/PronunciationResult.java
                                coach/EvaluationService.java
```

每个业务域是独立的 bounded context，域内高内聚，域间通过应用服务接口通信。
**单进程 Spring Boot**，所有域共享同一个 main()，打成一个 JAR。

### 2. 去掉 Dotenv，统一用 Spring @Value

```java
// talktic 原版
private static final Dotenv dotenv = Dotenv.configure().directory("secret").load();
private final String apiKey = dotenv.get("BAILIAN_API_KEY", "");

// DDD 重构版
@Value("${bailian.api-key:}")
private String apiKey;
```

### 3. BailianConfig 从静态常量 → Spring Bean

```java
// talktic 原版
public class BailianConfig {
    public static final String API_KEY = "sk-xxx";
}

// DDD 重构版
@Configuration
@ConfigurationProperties(prefix = "bailian")
@Data
public class BailianConfig {
    private String apiKey;
    private String wsUrl;
    private String voice;
}
```

### 4. 域内直接调用，无需跨域接口

chat + evaluation 合并为 coach 域后，评分服务与对话服务在同一个包内，直接方法调用：

```
ChatWebSocketHandler (coach)
  → EvaluationService.evaluateTurn() (coach)
    → PronunciationEvaluationService (coach)
    → GrammarCorrectionService (coach)
```

### 5. 去掉截图/视觉功能

talktic 的 `sendImage` + `hasVisualKeywordInLatestUserText` 不迁移。

### 6. 提示词模板按场景拆分

talktic 只有一个 `default.txt`，本项目按场景拆分 7 个模板。

---

## 新增文件清单

```
src/main/java/com/ackenieo/init_pro/
│
├── user/                                    ← 已有文件迁移
│   ├── User.java
│   ├── UserRepository.java
│   ├── UserDomainService.java
│   ├── UserDomainServiceImpl.java
│   ├── AuthService.java
│   ├── JwtTokenProvider.java
│   ├── SmsService.java
│   ├── UserMapper.java
│   ├── UserRepositoryImpl.java
│   ├── AuthController.java
│   ├── TestTokenController.java
│   ├── LoginRequest.java
│   ├── SendSmsRequest.java
│   └── RefreshTokenRequest.java
│
├── coach/                                   ← talktic 重构 + 新增
│   ├── ChatSession.java                              ★新增
│   ├── ChatMessage.java                              ★新增
│   ├── ChatSessionRepository.java                    ★新增
│   ├── ChatSessionRepositoryImpl.java                ★新增
│   ├── ChatSessionMapper.java                        ★新增
│   ├── ChatMessageMapper.java                        ★新增
│   ├── ConversationMemoryService.java                ★从talktic
│   ├── AudioTurnBufferService.java                   ★从talktic
│   ├── PromptTemplateService.java                    ★从talktic
│   ├── ChatSessionService.java                       ★新增
│   ├── RedisChatMemory.java                          ★从talktic
│   ├── PronunciationResult.java                      ★从talktic
│   ├── EvaluationService.java                        ★新增
│   ├── EvaluationConfig.java                         ★新增
│   ├── PronunciationEvaluationService.java           ★从talktic
│   ├── GrammarCorrectionService.java                 ★从talktic
│   ├── BailianConfig.java                            ★从talktic
│   ├── ChatMemoryConfig.java                         ★从talktic
│   ├── WebSocketConfig.java                          ★从talktic
│   ├── BailianRealtimeClient.java                    ★从talktic
│   ├── ChatWebSocketHandler.java                     ★从talktic
│   ├── WebSocketAuthInterceptor.java                 ★新增
│   └── ChatController.java                           ★新增
│
├── shared/                                  ← 已有文件迁移
│   ├── BaseEntity.java
│   ├── BaseValueObject.java
│   ├── BaseRepository.java
│   ├── DomainEvent.java
│   ├── DomainEventPublisher.java
│   ├── BaseCommand.java
│   ├── BaseQuery.java
│   ├── UseCaseResult.java
│   ├── DddArchitectureConfig.java
│   ├── MybatisPlusConfig.java
│   ├── RedissonConfig.java
│   ├── SecurityConfig.java
│   ├── SpringDomainEventPublisher.java
│   ├── BaseController.java
│   ├── ApiResponse.java
│   └── GlobalExceptionHandler.java
│
└── InitProApplication.java

src/main/resources/
├── prompts/                                 ★新增
│   ├── default.txt
│   ├── hotel.txt
│   ├── restaurant.txt
│   ├── airport.txt
│   ├── shopping.txt
│   ├── hospital.txt
│   └── business.txt
└── mapper/                                  ★新增
    ├── ChatSessionMapper.xml
    └── ChatMessageMapper.xml
```
