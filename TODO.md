# TODO

本文件用于 Claude / Codex 本地接力。每轮只处理一个名称明确、范围可验收的任务；未被用户确认前，不自动进入下一项。

## P0

### P0-1 项目定位去虚夸 + 文档基线补齐

- 状态：已完成
- 目标：
  - 创建 `TODO.md`
  - 填写 `HANDOFF.md` 本轮审查记录
  - 修正 `README.md` 中可能造成误解的 AI Agent 表述
  - 修复 `AGENTS.md` 中对不存在文档的引用
  - 确保 `docs/` 中各文档边界说明与源码一致
- 涉及文件：
  - `README.md`
  - `HANDOFF.md`
  - `TODO.md`
  - `AGENTS.md`
  - `docs/*.md`
- 禁止修改：
  - Java 源码
  - Vue 源码
  - SQL
  - `pom.xml`
  - `package.json` / `package-lock.json`
  - 配置文件
  - 测试文件

### P0-2 修复 variables_json 命名与真实 JSON 存储问题

- 状态：已完成
- 目标：
  - 将 `variables_json` 字段改为存储真正的 JSON 数组，或重命名字段以匹配当前真实格式，降低面试风险。
- 结果：
  - 采用方案 A，保留字段名并改为标准 JSON 数组存储。
  - Java 侧使用 Jackson 序列化 / 反序列化。
  - 前端接口 `variables` 数组结构保持不变。

## P1

### P1-1 补全局异常处理器

- 状态：已完成
- 结果：
  - 新增 `GlobalExceptionHandler`，统一处理 `ResponseStatusException`、参数校验异常和兜底异常。
  - API 错误响应统一为 `code` / `message` / `timestamp`，参数校验错误额外返回字段级 `errors`。
  - 后端测试基线更新为 13 个。

### P1-2 增加分页支持

- 状态：待执行

### P1-3 补关键日志点

- 状态：已完成

## P2

### P2-1 补 Springdoc OpenAPI 接口文档

- 状态：已完成

### P2-2 前端路由化重构或 App.vue 拆分

- 状态：待执行
