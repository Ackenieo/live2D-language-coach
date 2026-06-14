# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Development commands

### Backend (Spring Boot)
- Start local infrastructure: `docker-compose up -d`
- Run the backend: `./mvnw spring-boot:run`
  - Windows wrapper: `./mvnw.cmd spring-boot:run`
- Run all backend tests: `./mvnw test`
- Run a single backend test class: `./mvnw -Dtest=InitProApplicationTests test`
- Run a single backend test method: `./mvnw -Dtest=InitProApplicationTests#contextLoads test`
- Build the backend jar: `./mvnw package`

### Frontend prototype (`frontend/test`)
- Install dependencies: `cd frontend/test && npm install`
- Start the Vite dev server: `cd frontend/test && npm run dev`
- Build the frontend: `cd frontend/test && npm run build`
- Preview the production build: `cd frontend/test && npm run preview`

## Architecture overview

This repository is primarily a Spring Boot backend organized as a DDD-style layered application. The main dependency direction is:

`interfaces -> application -> domain <- infrastructure`

### Backend structure
- `src/main/java/com/ackenieo/init_pro/InitProApplication.java` is the Spring Boot entry point and scans MyBatis mapper interfaces from `infrastructure.mapper`.
- `interfaces` is the HTTP-facing layer. Controllers are thin and delegate to application services. Shared response shaping lives in `interfaces/base/BaseController.java`, `interfaces/dto/ApiResponse.java`, and `interfaces/exception/GlobalExceptionHandler.java`.
- `application` orchestrates use cases and cross-layer flows. For example, `application/service/AuthService.java` coordinates phone validation, SMS verification, user lookup/creation, and JWT issuance.
- `domain` holds business entities and contracts. `domain/model/entity/User.java` is the core user aggregate here, while repository abstractions such as `domain/repository/UserRepository.java` keep persistence concerns out of the domain layer.
- `infrastructure` contains framework and external-system implementations:
  - persistence via MyBatis-Plus (`infrastructure/mapper/UserMapper.java`, `infrastructure/repository/UserRepositoryImpl.java`)
  - Redis-backed SMS code storage and external SMS calls (`infrastructure/external/SmsService.java`)
  - JWT generation and validation (`infrastructure/security/JwtTokenProvider.java`)
  - Spring/MyBatis/Redis wiring (`infrastructure/config/*.java`)

### Auth flow to understand before changing related code
The implemented end-to-end path is easier to follow across layers than by directory alone:
1. `interfaces/rest/AuthController.java` exposes `/api/auth/*` endpoints.
2. `application/service/AuthService.java` handles the login/SMS/refresh use cases.
3. `domain/service/impl/UserDomainServiceImpl.java` applies user-related domain behavior such as auto-creating a user on first login.
4. `infrastructure/repository/UserRepositoryImpl.java` persists and queries users through MyBatis-Plus.
5. `infrastructure/external/SmsService.java` stores verification codes in Redis and sends SMS through an external HTTP endpoint.
6. `infrastructure/security/JwtTokenProvider.java` issues and validates access/refresh tokens.

### Frontend note
- `frontend/test` is a separate Vue 3 + Vite client/prototype, not part of the Maven build.
- Its auth client lives in `frontend/test/src/api/auth.js` and its main login screen is `frontend/test/src/LoginPage.vue`.
- Treat it as an independent frontend that talks to the backend auth endpoints rather than as a fully integrated monorepo app.

## Runtime/configuration facts
- Spring defaults to the `dev` profile via `src/main/resources/application.yml`.
- The backend listens on port `8520` in both `application-dev.yml` and `application-prod.yml`.
- Local defaults expect:
  - MySQL on `localhost:3381`
  - Redis on `localhost:6454`
- `docker-compose.yml` starts MySQL and Redis only; it does not run the Spring Boot app.
- MyBatis-Plus configuration in `infrastructure/config/MybatisPlusConfig.java` enables MySQL pagination, mapper XML scanning, logical delete fields, and automatic `createdAt`/`updatedAt` filling.

## Repository-specific cautions
- The root `package.json` is not the main project entry point for development; it only contains a placeholder `test` script and a small standalone dependency.
- The README describes a broader DDD template structure than what is fully implemented today. When documenting or extending the code, prefer the currently implemented auth/user flow and real package contents over the template description alone.


## 核心原则

0. **简单实现**: 所有功能实现都必须简单，思路清晰，不能过度复杂
1. **Web 项目测试**: 每次后端业务功能实现后必须使用 `curl` 调用 API 测试是否能正常使用
2. **临时脚本**: 所有临时脚本文件必须创建在 `./script_home/` 目录下
3. **数据文件**: 数据库创建后的 SQL/JS 等文件放在 `./live2D-language-coach/data/` 目录下
4. **复杂任务**: 分析任务后对于复杂任务应当使用 OpenSpec 的 skills
5. **策划书探索**: 对于 `./live2D-language-coach/docs/designed/` 下的策划书内容，如有不足之处，应当使用 OpenSpec 的 explore 工作流先探索
6. **危险指令**: 使用危险指令应当三思而后行
7. **技术栈完善**: 技术选项不足或缺失之处应该自己增加修改
8. **Skill 管理**: 在 `./skills/` 目录下增加新的 skill
9. **子 Agent 使用**: 尽量使用子 Agent 防止上下文爆炸
