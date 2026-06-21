# Claude / Codex 交接记录

使用规则：每轮把新记录追加在“历史记录”顶部，不覆盖旧记录。没有证据时不要写“测试通过”。

## 当前待处理交接

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
