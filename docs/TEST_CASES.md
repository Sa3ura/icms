# 测试用例与验证说明

## 1. 测试目标

本文档说明 ICMS 的测试范围、GitHub Actions 自动化验证项和核心业务测试用例。测试目标是证明系统满足以下交付要求：

- 核心业务流程可用：登录、预约、审批、工单、日志等功能能形成闭环。
- 权限与认证有效：未登录、角色不匹配、刷新令牌等场景有后端保护。
- 数据可持久化：业务状态、操作日志和统计数据来自数据库。
- 前后端可构建：前端生产包、后端测试与验证流程可在 CI 中复现。
- 线上问题可追踪：操作日志与 Sentry 监控能提供问题定位依据。

## 2. 测试环境

| 项目 | 配置 |
|---|---|
| CI 平台 | GitHub Actions |
| 触发方式 | push / pull request 到 `master` |
| 后端运行环境 | Ubuntu Latest, Java 17, Maven Wrapper |
| 前端运行环境 | Ubuntu Latest, Node.js 20, npm ci |
| E2E 浏览器 | Playwright Chromium |
| 真实 E2E 数据库 | MySQL 8.0 service |
| 真实 E2E 后端 | GitHub Actions 中启动 Spring Boot 服务 |

## 3. GitHub Actions 自动化验证项

CI 配置文件位于 `.github/workflows/ci.yml`。当前流水线包含以下 job：

| Job | 命令 / 动作 | 验证内容 | 产物 |
|---|---|---|---|
| `backend` | `./mvnw test -B` | 后端单元测试 | `backend-test-report` |
| `backend-it` | `./mvnw verify -B` | 后端集成测试 / Maven verify 阶段 | `backend-it-report` |
| `frontend-unit` | `npm ci` | 前端依赖可复现安装 | - |
| `frontend-unit` | `npm run lint` | Vue / JS 代码规范检查 | - |
| `frontend-unit` | `npm test` | Vitest 前端单元测试 | - |
| `frontend-unit` | `npm run build` | 前端生产构建 | - |
| `frontend-e2e` | `npm run test:e2e` | Playwright mock E2E 流程 | `e2e-screenshots` |
| `frontend-e2e-real` | `npx playwright test --config e2e/playwright.real.config.js --reporter=line` | 连接 MySQL 和真实后端的 E2E 流程 | `e2e-real-screenshots` |

答辩时可使用 GitHub Actions 的绿色通过记录、测试报告 artifact 和 Playwright 截图作为测试证据。

## 4. 后端自动化测试用例

| 编号 | 测试文件 | 模块 | 验证点 | 状态 |
|---|---|---|---|---|
| BE-AUTH-001 | `AuthControllerTest` | 登录认证 | 登录、验证码、认证接口响应 | 已自动化 |
| BE-DASH-001 | `DashboardControllerTest` | 仪表盘 | 统计指标和周使用率接口结构 | 已自动化 |
| BE-USER-001 | `UserControllerTest` | 用户管理 | 用户相关接口和权限边界 | 已自动化 |
| BE-WO-001 | `WorkOrderControllerTest` | 工单管理 | 工单接口流程和状态响应 | 已自动化 |
| BE-WO-002 | `WorkOrderServiceImplTest` | 工单服务 | 工单业务逻辑处理 | 已自动化 |
| BE-NOTICE-001 | `NoticeControllerTest` | 通知管理 | 通知接口返回和业务约束 | 已自动化 |
| BE-LOG-001 | `LogAspectTest` | 操作日志 | 操作日志切面记录行为 | 已自动化 |
| BE-IP-001 | `RequestClientUtilTest` | 客户端 IP | 代理头、CDN、无 CDN 场景下真实 IP 解析 | 已自动化 |

## 5. 前端自动化测试用例

| 编号 | 测试文件 / 命令 | 模块 | 验证点 | 状态 |
|---|---|---|---|---|
| FE-UNIT-001 | `src/__tests__/mappings.test.js` | 前端映射 | 状态、角色、标签等映射逻辑稳定 | 已自动化 |
| FE-UNIT-002 | `src/__tests__/useTableSort.test.js` | 表格交互 | 表格排序组合逻辑 | 已自动化 |
| FE-LINT-001 | `npm run lint` | 前端工程 | Vue / JS 代码规范 | 已自动化 |
| FE-BUILD-001 | `npm run build` | 前端工程 | 生产构建可通过 | 已自动化 |
| FE-DASH-001 | 仪表盘页面 | 首页图表 | 周一至周日标签正常展示，不出现 `undefined` | 已修复并验证 |

## 6. E2E 流程测试用例

| 编号 | 测试文件 | 业务流程 | 验证点 | 状态 |
|---|---|---|---|---|
| E2E-LOGIN-001 | `01-login.spec.js` | 登录流程 | 登录页、表单提交、登录态跳转 | 已自动化 |
| E2E-RES-001 | `02-reservation.spec.js` | 教室预约 | 查询教室、提交预约、状态展示 | 已自动化 |
| E2E-APPROVAL-001 | `03-approval.spec.js` | 预约审批 | 审批列表与审批操作流程 | 已自动化 |
| E2E-WO-001 | `04-workorder.spec.js` | 工单报修 | 报修提交、工单处理链路 | 已自动化 |
| E2E-USER-001 | `05-user-management.spec.js` | 用户管理 | 用户页面基础管理流程 | 已自动化 |
| E2E-REAL-001 | `06-real-login.spec.js` | 真实后端登录 | MySQL + Spring Boot + 前端真实链路登录 | 已自动化 |

## 7. 人工验证与演示用例

以下用例用于部署后验收和答辩演示。它们不全部等同于自动化测试，但可通过线上环境截图或现场演示证明。

| 编号 | 模块 | 操作步骤 | 预期结果 | 验证方式 |
|---|---|---|---|---|
| MANUAL-LOGIN-001 | 登录认证 | 输入账号、密码、验证码并登录 | 进入首页，用户信息加载成功 | 人工验证 |
| MANUAL-DASH-001 | 仪表盘 | 打开首页查看统计卡片和周使用率 | 数据正常展示，无 `undefined` | 人工验证 |
| MANUAL-CLASS-001 | 教室管理 | 新增或编辑教室信息 | 列表刷新后展示最新数据 | 人工验证 |
| MANUAL-RES-001 | 预约管理 | 普通用户提交预约，教务角色审批 | 预约状态正确流转 | 人工验证 |
| MANUAL-WO-001 | 设备报修 | 提交报修并处理工单 | 工单状态正确更新 | 人工验证 |
| MANUAL-LOG-001 | 操作日志 | 执行业务操作后查看日志 | 记录操作用户、接口、结果和真实 IP | 人工验证 |
| MANUAL-SENTRY-001 | Sentry 监控 | 触发测试异常 | Sentry 能看到对应错误事件 | 人工验证 |

## 8. 测试结论

ICMS 当前已通过 GitHub Actions 覆盖后端单元测试、后端集成测试、前端 lint、前端单元测试、前端构建、Playwright mock E2E 和真实后端 E2E。结合人工部署验证，系统已覆盖登录认证、教室预约、审批、工单、用户管理、操作日志、仪表盘和监控等核心场景，满足答辩演示与基础交付验证要求。

后续可继续补充更细粒度的前端组件测试、更多跨角色 E2E 场景、CI/CD 自动部署和 Sentry Release / Source Map 验证。
