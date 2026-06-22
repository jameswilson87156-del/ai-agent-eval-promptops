# Claude / Codex 交接记录

使用规则：每轮把新记录追加在“历史记录”顶部，不覆盖旧记录。没有证据时不要写“测试通过”。

## 当前待处理交接

### 2026-06-22 — Codex — P2-1 补 Springdoc OpenAPI 接口文档

- 当前分支：`resume-optimization-v1`
- 本轮任务：P2-1 补 Springdoc OpenAPI 接口文档。
- 修改文件：`backend/pom.xml`、`backend/src/main/java/com/promptops/evalconsole/api/PromptOpsController.java`、`backend/src/main/resources/application.yml`、`README.md`、`TODO.md`、`HANDOFF.md`、`docs/architecture.md`

### P2-1 实现内容

- 新增 Maven 依赖：`org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0`。
- `PromptOpsController` 增加类级别 `@Tag`，并为全部 11 个接口方法增加 `@Operation(summary = "...")`。
- `application.yml` 增加 Springdoc 路径配置：`/api-docs` 和 `/swagger-ui.html`，并按 method 排序。
- `README.md` 增加本地 Swagger UI 访问说明：`http://localhost:18080/swagger-ui.html`。
- `docs/architecture.md` 补充 OpenAPI / Swagger UI 是接口展示和协作能力，不是 AI 能力。
- 未修改业务逻辑、接口路径、方法签名、参数、返回类型或前端代码。

### P2-1 测试与验证

- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改 Service / Mapper / Entity，未改 `ConsoleDtos.java`，未改 `GlobalExceptionHandler.java`，未改前端 Vue，未改数据库 schema。
- 未安装 npm 依赖，未启动后端、前端、外部数据库或 Docker 服务；仅运行 Maven 测试内存 H2 上下文。

### P2-1 下一轮唯一建议任务

P2-2 README / GitHub 展示强化。理由：OpenAPI 文档已经补齐，下一步先把 GitHub 展示路径、截图和本地访问说明打磨清楚，更能承接本轮面试展示目标；P1-2 分页支持仍有价值，但当前 demo 数据量小，紧急度较低。

### 2026-06-22 — Codex — P1-3 补关键日志点

- 当前分支：`resume-optimization-v1`
- 本轮任务：P1-3 补关键日志点。
- 修改文件：`backend/src/main/java/com/promptops/evalconsole/service/PromptOpsService.java`、`backend/src/main/java/com/promptops/evalconsole/service/RuleEvaluator.java`、`backend/src/main/java/com/promptops/evalconsole/service/DemoDataInitializer.java`、`TODO.md`、`HANDOFF.md`、`docs/architecture.md`

### P1-3 实现内容

- `PromptOpsService` 新增 `Logger`，记录 Eval Run 开始时的 `promptVersionId`、`datasetId`、`caseCount`，以及完成时的 `runKey`、`passedCases`、`failedCases`、`passRate`。
- `PromptOpsService` 在 `toVariablesJson()` 序列化失败时记录 `log.error`，只记录变量数量和异常；在 `variables()` 解析失败且即将抛出 `IllegalStateException` 前记录 `log.warn`，只记录截断摘要。
- `RuleEvaluator` 新增 `Logger`，当规则评分产生失败项时记录 debug 日志，包含 `status`、失败数量和 `score`。
- `DemoDataInitializer` 新增 `Logger`，记录 demo 数据因配置关闭或已有模板而跳过初始化、三个种子模板完成写入，以及初始化完成后的 profile 数量。
- 未修改业务逻辑，未修改 API 合约，未新增依赖。

### P1-3 测试与验证

- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改前端 Vue，未改 `pom.xml`，未改 `package.json` / `package-lock.json`，未改数据库 schema。
- 未安装依赖，未启动后端、前端、外部数据库或 Docker 服务；仅运行 Maven 测试内存 H2 上下文。

### P1-3 下一轮唯一建议任务

P1-2 增加分页支持。理由：当前服务层关键日志已经补齐，下一步先补列表分页能降低数据增长后的 API 风险；OpenAPI 文档更适合在分页合约确定后再补。

### 2026-06-22 — Codex — P1-1 补全局异常处理器

- 当前分支：`resume-optimization-v1`
- 本轮任务：P1-1 补全局异常处理器。
- 新增文件：`backend/src/main/java/com/promptops/evalconsole/api/GlobalExceptionHandler.java`
- 修改文件：`backend/src/main/java/com/promptops/evalconsole/api/dto/ConsoleDtos.java`、`backend/src/test/java/com/promptops/evalconsole/EvalConsoleApplicationTests.java`、`TODO.md`、`HANDOFF.md`、`docs/architecture.md`、`docs/acceptance-checklist.md`、`README.md`

### P1-1 实现内容

- 使用 `@RestControllerAdvice` 新增全局异常处理器。
- 统一处理 `ResponseStatusException`：使用异常自身 HTTP status 和 reason。
- 统一处理 `MethodArgumentNotValidException`：返回 400、`Validation failed` 和字段级 `errors`。
- 统一处理通用 `Exception`：返回 500 和通用消息 `Internal server error`，不暴露堆栈、SQL、类名等内部细节。
- 在 `ConsoleDtos` 中新增 `ErrorResponse` 和 `FieldErrorItem` record。

### P1-1 ErrorResponse 结构

- `code`：HTTP 状态码数字。
- `message`：可展示错误信息。
- `timestamp`：`LocalDateTime` 时间戳。
- `errors`：字段级错误列表；非校验错误为空时不输出。

### P1-1 测试与验证

- 新增测试：`returnsUnifiedErrorForMissingPromptTemplate`，覆盖 404 `ResponseStatusException` 统一响应。
- 新增测试：`returnsFieldErrorsForInvalidPromptTemplateRequest`，覆盖 400 参数校验错误和字段级 `errors`。
- 已执行：`mvn -pl backend test`。
- 结果：BUILD SUCCESS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- 未改前端 Vue，未改 `pom.xml`，未改 `package.json` / `package-lock.json`，未改数据库 schema。
- 未安装依赖，未启动后端、前端、数据库或 Docker。

### P1-1 下一轮唯一建议任务

P1-3 补关键日志点。理由：异常响应已经统一，下一步补服务层关键业务日志能继续提升 Java 后端工程可信度；分页支持也有价值，但当前数据量小，日志对面试解释和问题定位更直接。

### 2026-06-22 — Codex — P0-2 修复 variables_json 命名与真实 JSON 存储问题

- 当前分支：`resume-optimization-v1`
- 本轮任务：P0-2 修复 `variables_json` 命名与真实 JSON 存储问题
- 方案选择：方案 A，保留 `variables_json` 字段名并改为标准 JSON 数组存储。
- 选择理由：MySQL schema 已使用 JSON 类型，项目已有 Jackson；改成真实 JSON 数组能让字段名、数据库设计、Java 序列化逻辑和面试解释一致，同时保持前端接口不变。

### P0-2 修改文件

- `backend/src/main/java/com/promptops/evalconsole/service/PromptOpsService.java`：`toVariablesJson()` 改为 Jackson JSON 序列化，`variables()` 改为 Jackson JSON 反序列化，并兼容旧管道格式读取。
- `backend/src/main/java/com/promptops/evalconsole/persistence/entity/PromptVersionEntity.java`：补充 `variablesJson` 字段语义注释。
- `backend/src/main/resources/schema.sql`：补充 `variables_json` 存储标准 JSON 数组的注释。
- `backend/src/main/resources/schema-h2.sql`：补充 H2 使用 CLOB 承载 JSON 字符串的注释。
- `backend/src/test/resources/schema-h2.sql`：补充 H2 测试 schema 注释。
- `backend/src/test/java/com/promptops/evalconsole/EvalConsoleApplicationTests.java`：新增 JSON 数组写入与旧管道格式兼容读取测试。
- `TODO.md`：将 P0-1、P0-2 状态更新为已完成。
- `docs/architecture.md`、`docs/interview-guide.md`、`docs/acceptance-checklist.md`、`README.md`：同步当前 JSON 存储和测试基线。
- `HANDOFF.md`：追加本轮交接记录。

### P0-2 结果与边界

- `variables_json` 当前已经用于存储真实 JSON 数组，例如 `[{"name":"customerType","description":"客户类型","required":true}]`。
- Java 侧使用 Jackson 序列化 / 反序列化 `List<PromptVariableDto>`。
- 旧数据如 `[name::description::true|risk::风险等级::false]` 仍可兼容解析。
- 前端接口保持不变，仍返回 `variables` 数组；未修改 Vue 源码。
- 未修改 `package.json` / `package-lock.json` / `pom.xml`。
- 未安装依赖，未启动后端、前端、数据库或 Docker。

### P0-2 验证结果

- 已执行：`mvn -pl backend test`
- 结果：BUILD SUCCESS，Tests run: 11, Failures: 0, Errors: 0, Skipped: 0。
- 已执行：`git diff --check`，结果退出码 0；仅有 Windows 换行符提示。
- 已执行：`git status`，结果仅显示本轮相关文件 modified，未暂存。
- 已执行本轮 diff 范围检查，未出现前端 Vue、`package.json` / `package-lock.json` 或 `pom.xml` 改动。

### P0-2 下一轮唯一建议任务

P1-1 补全局异常处理器。P0 风险修完后，下一步应提升 Java 后端工程可信度，让项目更适合写进简历。

### 2026-06-21 — Codex — P0-1 项目定位去虚夸 + 文档基线补齐

- 当前分支：`resume-optimization-v1`
- 已有提交：
  - `d56decc init ai agent eval promptops project`
  - `1ef1885 add claude codex collaboration rules`
- 本轮任务：P0-1 项目定位去虚夸 + 文档基线补齐
- 本轮修改范围：
  - `README.md`
  - `HANDOFF.md`
  - `TODO.md`
  - `AGENTS.md`
  - `docs/*.md`

### 项目真实定位

PromptOps 本地实验台 / 规则评测工作台 / Eval Run + Case Result + Generation Trace 落库闭环。

### 项目真实能力

- Prompt 模板与多版本管理
- Eval Dataset / Eval Case / ScoreRule / EvalRun / EvalCaseResult / GenerationTrace 数据建模
- RuleEvaluator 可解释规则评分
- EvaluationPolicy Review 队列判断与版本回归风险分级
- MySQL + H2 demo/test profile
- Vue 3 + TypeScript 前端真实 fetch 后端 API
- 后端测试覆盖规则评分和评测主流程

### 明确边界

- 无真实 LLM 调用
- 无真实 AI Agent
- 无多模型对比
- 无 LLM-as-Judge
- MockOutputGenerator 是确定性 Mock，输出和模拟耗时由版本号与 caseKey 等本地规则决定
- RuleEvaluator 是规则评分，不是 AI 语义评分
- Review 状态部分仍是前端演示状态，不是持久化审批流
- `variables_json` 目前不是真正 JSON，这是 P0-2 风险项

### 本轮发现的文档问题

- `TODO.md` 不存在
- `HANDOFF.md` 是空模板
- `AGENTS.md` 的“开始工作前必须读取”列表引用了不存在的 `docs/PRD.md`、`docs/DESIGN.md` 和 `docs/ARCHITECTURE.md`
- `README.md` 副标题“AI Agent 评测中心”容易造成误解

### 本轮只读审查记录

- 已执行：`git status`
- 已执行：`git log --oneline -5`
- 已读取：`AGENTS.md`、`CLAUDE.md`、`HANDOFF.md`、`README.md`、`docs/architecture.md`、`docs/interview-guide.md`、`docs/demo-script.md`、`docs/acceptance-checklist.md`、`templates/codex-task-template.md`
- 已核对源码边界：`MockOutputGenerator`、`RuleEvaluator`、`EvaluationPolicy`、`PromptOpsService` 与 schema 中的 `variables_json` 相关字段
- 未发现本轮开始前已有业务文件 diff；`git diff --stat` 仅输出 Windows 换行符提示

### 本轮未做事项

- 未安装依赖
- 未启动服务
- 未运行构建
- 未运行测试
- 未修改核心业务代码
- 未修改 Java / Vue / SQL / `pom.xml` / `package.json`

### 下一轮唯一建议任务

P0-2 修复 `variables_json` 命名与真实 JSON 存储问题。

## Codex 修复结果

- 修复时间：2026-06-21（Asia/Shanghai）
- 修改文件：`README.md`、`HANDOFF.md`、`TODO.md`、`AGENTS.md`、`docs/*.md`
- 修复说明：补齐任务队列和交接记录，修正容易被理解为真实 AI Agent / LLM Eval 系统的文档表述，并补充 Mock、规则评分、Review、Profile 与不支持项边界。
- 实际运行命令：
  - `git diff -- README.md docs HANDOFF.md TODO.md AGENTS.md CLAUDE.md templates`
  - `git diff --check -- README.md docs HANDOFF.md TODO.md AGENTS.md CLAUDE.md templates`
  - `git status`
- 实际结果：
  - diff 仅涉及文档类文件：`AGENTS.md`、`HANDOFF.md`、`README.md`、`TODO.md`、`docs/acceptance-checklist.md`、`docs/architecture.md`、`docs/demo-script.md`、`docs/interview-guide.md`
  - `git diff --check` 退出码 0；仅有 Windows 换行符提示
  - `git status` 显示上述文件未暂存，`TODO.md` 为 untracked；未看到 `.gitignore` modified
- 未运行的验证及原因：按本轮限制，不运行 `mvn test`、`mvn package`、`npm install`、`npm run build`，也不启动后端、前端、数据库或 Docker。
- 剩余风险：前端页面和部分种子数据仍可能出现 “AI Agent” 字样，但本轮明确禁止修改 Vue / Java 源码，留待后续任务确认是否处理。

## 历史记录

暂无历史记录。
