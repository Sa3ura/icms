# ICMS — 智慧教室管理系统

Intelligent Classroom Management System — 基于 Vue 3 + Spring Boot 的全栈智慧校园教室资源管理平台。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | Vue 3 (Composition API) | ^3.5 |
| 构建 | Vite | ^8.0 |
| 路由 | Vue Router | ^5.1 (Hash 模式) |
| 状态 | Pinia | ^3.0 |
| UI 框架 | Element Plus | ^2.14 |
| 图表 | ECharts (按需引入) | ^6.1 |
| HTTP | Axios | ^1.18 |
| 样式 | Sass + CSS 自定义属性 | — |
| **后端** | Spring Boot | 3.2.4 |
| ORM | MyBatis-Plus | 3.5.5 |
| 认证 | SaToken (JWT 无状态) | 1.38.0 |
| 安全 | BCrypt + SVG 验证码 + IP 限流 + SaMode.OR | — |
| 文档 | Knife4j + OpenAPI 3 (Swagger) | 4.5.0 |
| 日志 | Log4j2 (彩色控制台 + SQL/App 分离滚动文件) | — |
| 数据库 | MySQL + Flyway 版本迁移 | 8.x |
| 构建 | Maven Wrapper | 3.9.6 |

## 项目结构

```
ICMS/
├── ICMS-frontend/              # Vue 3 前端
│   ├── src/
│   │   ├── api/index.js        # API 层（Mock + 真实后端双模式）
│   │   ├── db/mockDb.js        # localStorage Mock 数据库
│   │   ├── router/index.js     # 路由 + 角色权限守卫
│   │   ├── store/user.js       # Pinia 用户/主题状态
│   │   ├── layout/index.vue    # 主布局（侧边栏+顶栏+内容）
│   │   ├── composables/        # 组合式函数
│   │   ├── utils/mappings.js   # 枚举映射工具
│   │   └── views/              # 页面组件
│   │       ├── Login.vue
│   │       ├── dashboard/      # 首页仪表盘
│   │       ├── classroom/      # 教室管理 + 空闲查询
│   │       ├── reservation/    # 预约申请 + 审批
│   │       ├── device/         # 设备资产管理
│   │       ├── workorder/      # 报修工单
│   │       ├── environment/    # 环境实时监测
│   │       └── system/         # 用户管理 + 操作日志 + 通知管理
│   ├── .env.development        # 开发环境变量（Mock 模式开启）
│   ├── .env.production         # 生产环境变量
│   └── eslint.config.js
│
├── ICMS-backend/               # Spring Boot 后端
│   ├── src/main/java/com/icms/
│   │   ├── controller/         # REST 控制器 (11 个)
│   │   ├── service/            # 业务服务层
│   │   ├── mapper/             # MyBatis-Plus 数据访问
│   │   ├── entity/             # 数据实体 (13 个)
│   │   ├── config/             # Spring 配置 + DatabaseInitializer
│   │   ├── security/           # SaToken / 验证码 / 登录限流
│   │   └── common/             # 统一响应 / 异常处理 / AOP 日志 / 驼峰转蛇形
│   └── src/main/resources/
│       ├── db/migration/       # Flyway 版本化 SQL 迁移
│       └── application.yml
│
├── ICMS_database_schema_简体.sql  # 数据库建表脚本
├── ICMS_需求设计文档.md
├── ICMS_数据库表设计.md
├── ICMS_功能列表清单.md
├── ICMS_页面白板原型.md
└── ICMS_交付差距分析.md         # 上线前待修复清单
```

## 功能模块

| 模块 | 路由 | 功能 | 角色 |
|------|------|------|------|
| 仪表盘 | `/dashboard` | 统计卡片 + 空间活跃度图表 + 设备状态饼图 + 今日占用 | 全部 |
| 教室管理 | `/classroom/list` | CRUD + 校区/楼栋/楼层级联 + 课表沙盘 + 设备快查 | admin/jw/teacher |
| 空闲查询 | `/classroom/free` | 按日期/节次/容量/设备过滤 + 一键预约 | 全部 |
| 我的预约 | `/reservation/my` | 新建申请 + 冲突实时检测 + 取消 | 全部 |
| 预约审批 | `/reservation/approval` | 通过/驳回 + 审批意见 | admin/jw |
| 设备管理 | `/device/list` | CRUD + 快速报修跳转工单 | admin/jw/teacher |
| 工单管理 | `/work-order/list` | 提交→指派→处理→关闭全生命周期 | 全部 |
| 环境监测 | `/environment/index` | 6 项指标实时卡片 + 温湿度/空气质量双图表 | admin/jw/teacher |
| 用户管理 | `/system/user` | CRUD + 启用/禁用 + 重置密码 | admin/jw |
| 通知管理 | `/system/notices` | 发布/编辑/删除通知 + 已读统计 | admin/jw |
| 操作日志 | `/system/log` | 审计日志检索 | admin |

## 快速开始（前端 Mock 模式）

无需后端和数据库，纯前端运行：

```bash
cd ICMS-frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`，默认使用 Mock 数据（localStorage）。

## 切换真实后端

修改 `.env.development`：

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080
```

生产构建使用同源 `/api` 反向代理时，保持 `.env.production` 中 `VITE_API_BASE_URL=` 为空即可。前端请求路径本身已包含 `/api` 前缀，避免配置为 `/api` 后生成 `/api/api/...`。

## 后端启动

```bash
cd ICMS-backend

# 1. 创建数据库（仅需一次）
# mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS icms DEFAULT CHARACTER SET utf8mb4;"

# 2. 修改 application.yml 中的数据库连接信息

# 3. 启动（Flyway 自动建表 + DatabaseInitializer 播种数据）
./mvnw spring-boot:run   # Linux/Mac
mvnw.cmd spring-boot:run # Windows
```

启动时 Flyway 自动执行 `db/migration/` 下的版本化 SQL 脚本，无需手动导入建表文件。

启动后访问 `http://localhost:8080/doc.html` 可查看 Knife4j API 文档（各接口带中文 `@Tag` / `@Operation` 模块与摘要）。

## 默认账户

所有种子用户密码统一为 `123456`：

| 用户名 | 角色 | 说明 |
|--------|------|------|
| `admin` | 系统管理员 | 全部权限 |
| `jw_admin` | 教务管理员 | 教室/预约/审批 |
| `teacher_wang` | 教师 | 预约/报修 |
| `student_li` | 学生 | 查询/预约 |
| `repairer_zhang` | 维修人员 | 工单处理 |

## 开发命令

```bash
# 前端
npm run dev        # 开发服务器
npm run build      # 生产构建
npm run preview    # 预览生产构建
npm run lint       # ESLint 检查
npm run format     # ESLint 自动修复
npm test           # Vitest 单元测试 (20 用例)
npm run test:e2e    # Playwright E2E 测试 (6 流程)

# 后端
mvnw.cmd spring-boot:run  # 启动后端
mvnw.cmd test              # JUnit5 单元测试 (17 用例)
```

## 用户角色

| 角色 | 代号 | 权限范围 |
|------|------|----------|
| 系统管理员 | `admin` | 全部功能 |
| 教务管理员 | `jw` | 教室管理、预约审批、设备管理、通知管理 |
| 教师 | `teacher` | 查询教室、申请预约、报修设备 |
| 学生 | `student` | 查询空闲教室、申请预约 |
| 维修人员 | `repairer` | 工单接收与处理 |

## 特性

- **双模式运行**：Mock 模式（纯前端 localStorage）与真实 API 模式一键切换
- **安全认证**：SaToken JWT 无状态 + `@SaCheckRole(value = {...}, mode = SaMode.OR)` 注解鉴权 + 双 Token (Access 内存/Refresh httpOnly Cookie) 防 XSS 窃取
- **越权防护**：写操作身份一律取自 `StpUtil.getLoginIdAsLong()`，不信任请求体传入的 id；预约取消/改密等敏感操作校验"本人或管理员"
- **令牌加固**：Refresh Token 以 SHA-256 摘要入库，原文仅存于 httpOnly Cookie，库表泄露不可反推
- **CORS 白名单**：`app.cors.allowed-origins` 配置化来源白名单，替代通配符，配合 `allowCredentials` 收紧跨域
- **登录安全**：后端 SVG 干扰验证码 + 同 IP 5次/5分钟失败封锁 15 分钟（毫秒时间戳全程 `long` 计算，规避 int 溢出）
- **密钥外置**：JWT 密钥与数据库口令移出源码，置于 gitignore 的 `application-local.yml`
- **API 文档**：Knife4j + OpenAPI 3，11 个 Controller/40 个接口全覆盖中文 `@Tag` 模块与 `@Operation` 摘要，`http://localhost:8080/doc.html`
- **暗黑模式**：CSS 自定义属性 + 平滑主题过渡动画 (`theme-transitioning`)
- **通知系统**：发布→推送→已读追踪（`read_by[]` 独立状态）+ 30s 自动轮询
- **ECharts 按需引入**：仅打包使用的 Line/Bar/Pie 图表 + SVGRenderer
- **操作栏优化**：≥3 按钮自动收进 `el-dropdown`，统一"操作 ▾"样式
- **滚动体验**：8px 滚动条 + Firefox 兼容 + 平滑滚动 + 表格独立横向滚动
- **Hash 路由**：无服务端 fallback 配置需求，刷新不 404
- **数据库版本管理**：Flyway `V1__init_schema.sql` → `V2__add_refresh_token.sql` 自动增量迁移

## 交付状态

已修复 **51 项**缺陷，致命安全漏洞 **4/4 全部清零**，编译/Lint 零错误。S1+S2+S3 全部完成。**单元测试 37 用例全绿**（后端 17 + 前端 20），**E2E 测试 6 条流程全绿**。

**安全加固轮次**（严重+中危）：登录限流 int 溢出修复、横向越权（IDOR）链路收敛、Refresh Token 哈希入库、CORS 来源白名单、工单指派空指针防护 — 后端 `BUILD SUCCESS`。

可作为安全可靠的原型/Demo 演示。

## 前端审查修复更新（2026-07-02）

- 生产环境 API baseURL 改为空同源模式，配合 nginx `/api` 反向代理，避免 `/api/api/...` 路径错误。
- `tryRefresh()` 成功后同步写入内存 access token，刷新页面后后续接口继续携带 `Authorization`。
- `useTableSort` 改为对当前页数据执行本地排序，7 个表格页面的排序按钮不再只是 UI 状态。
- 预约审批和操作日志搜索改为先拉足量数据再本地过滤分页，搜索结果与分页 `total` 保持一致。

验证命令：

```bash
cd ICMS-frontend
npm run lint
npm run build
```

当前前端验证结果：Lint 通过，生产构建通过；构建阶段仅有第三方依赖 pure annotation 与 chunk size 警告。

## 安全修复更新（2026-07-02）

- 登录限流已覆盖验证码失败和账号/密码失败；成功登录后才清除失败计数。
- 预约与工单列表接口按当前登录用户自动收敛数据范围：`admin`/`jw` 可查全量，普通用户仅查本人数据，`repairer` 仅查分配给自己的工单。
- 工单处理接口 `POST /api/work-orders/{id}/handle` 的 `status` 仅允许 `processing`、`finished`，且维修人员只能处理自己的工单。
- 预约审批接口 `POST /api/reservations/{id}/approve` 的 `status` 仅允许 `approved`、`rejected`，审批更新会校验记录仍处于 `pending`。
- 用户保存接口已改为字段白名单，忽略请求体中的 `password`、`refreshToken`、`deleted` 等敏感字段；密码变更只走修改/重置密码接口。
- Refresh Token Cookie 新增配置 `icms.refresh-token.secure`。本地默认 `false`；生产 HTTPS 环境应设为 `true`。

```yaml
icms:
  refresh-token:
    secure: true
```

验证命令：

```bash
cd ICMS-backend
mvnw.cmd test
```

当前后端测试结果：`Tests run: 5, Failures: 0, Errors: 0`。
