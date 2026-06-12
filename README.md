# live2D-language-coach

基于 DDD（领域驱动设计）邻域架构的 Spring Boot 3.5 项目，集成 MyBatis-Plus、Redis、MySQL。

## 技术栈

- **Spring Boot 3.5.14** - 应用框架
- **MyBatis-Plus 3.5.7** - 持久层框架
- **Redis** - 缓存/会话存储
- **MySQL 8.0** - 关系型数据库
- **Java 17** - 运行环境

## 项目结构

```
live2D-language-coach/
├── src/main/java/com/ackenieo/init_pro/
│   ├── domain/                          # 领域层（核心业务逻辑，无外部依赖）
│   │   ├── model/
│   │   │   ├── entity/
│   │   │   │   └── BaseEntity.java      # 实体基类（ID、审计字段、逻辑删除）
│   │   │   └── valueobject/
│   │   │       └── BaseValueObject.java # 值对象基类
│   │   ├── repository/
│   │   │   └── BaseRepository.java      # 仓储接口（领域层定义）
│   │   ├── service/                     # 领域服务（复杂业务逻辑）
│   │   └── event/
│   │       ├── DomainEvent.java         # 领域事件基类
│   │       └── DomainEventPublisher.java # 事件发布器接口
│   │
│   ├── application/                     # 应用层（用例编排，协调领域对象）
│   │   ├── usecase/
│   │   │   └── UseCaseResult.java       # 用例执行结果封装
│   │   ├── command/
│   │   │   └── BaseCommand.java         # 命令基类（写操作入参）
│   │   ├── query/
│   │   │   └── BaseQuery.java           # 查询基类（读操作入参）
│   │   └── service/                     # 应用服务（事务边界、权限校验）
│   │
│   ├── infrastructure/                  # 基础设施层（技术实现细节）
│   │   ├── repository/                  # 仓储实现（MyBatis-Plus Mapper）
│   │   ├── mapper/                      # MyBatis-Plus Mapper 接口
│   │   ├── config/
│   │   │   ├── DddArchitectureConfig.java # DDD 架构配置
│   │   │   └── MybatisPlusConfig.java   # MyBatis-Plus 配置（分页、自动填充）
│   │   ├── persistence/                 # 持久化相关（PO 对象、转换器）
│   │   ├── client/                      # 外部服务客户端（HTTP、RPC）
│   │   └── event/
│   │       └── SpringDomainEventPublisher.java # Spring 事件发布器实现
│   │
│   ├── interfaces/                      # 接口层（外部交互入口）
│   │   ├── rest/
│   │   │   └── BaseController.java      # REST 控制器基类
│   │   ├── dto/
│   │   │   └── ApiResponse.java         # 统一 API 响应封装
│   │   └── assembler/                   # 对象转换器（DTO ↔ Entity）
│   │
│   └── InitProApplication.java          # Spring Boot 启动类
│
├── src/main/resources/
│   ├── application.yml                  # 应用配置（端口、数据源、Redis、MyBatis-Plus）
│   └── mapper/                          # MyBatis XML 映射文件
│
├── docker-compose.yml                   # Docker 编排（MySQL、Redis）
├── Dockerfile                           # 应用容器化构建
└── pom.xml                              # Maven 依赖管理
```

## 架构说明

### 依赖方向

```
interfaces → application → domain ← infrastructure
```

- **领域层** 是核心，不依赖任何外层
- **应用层** 依赖领域层，编排用例
- **基础设施层** 实现领域层定义的接口（如仓储）
- **接口层** 处理外部请求，调用应用层

### 分层职责

| 层次 | 职责 | 示例 |
|------|------|------|
| **domain** | 业务规则、实体、值对象、领域事件 | 订单状态流转、金额计算 |
| **application** | 用例编排、事务管理、权限校验 | 创建订单、取消订单 |
| **infrastructure** | 数据库访问、缓存、外部服务调用 | MyBatis-Plus Mapper、Redis 操作 |
| **interfaces** | HTTP 接口、消息监听、定时任务 | REST Controller、DTO 转换 |

## 快速开始

### 1. 启动基础设施

```bash
docker-compose up -d
```

- MySQL: `localhost:3381` (root/root123456)
- Redis: `localhost:6454`

### 2. 启动应用

```bash
.\mvnw.cmd spring-boot:run
```

- 应用端口: `8520`

### 3. 验证

```bash
curl http://localhost:8520/
```

## Docker 配置

| 服务 | 容器端口 | 宿主机端口 | 偏移 |
|------|----------|------------|------|
| MySQL | 3306 | 3381 | +75 |
| Redis | 6379 | 6454 | +75 |
| App | 8520 | 8520 | - |

## 扩展指南

### 添加新业务模块

1. **领域层** - 在 `domain/model/entity` 创建实体，在 `domain/repository` 定义仓储接口
2. **基础设施层** - 在 `infrastructure/mapper` 创建 Mapper 接口，在 `infrastructure/repository` 实现仓储
3. **应用层** - 在 `application/service` 创建应用服务，编排用例
4. **接口层** - 在 `interfaces/rest` 创建 Controller，暴露 API
