# ICMS 交付差距分析报告

> 报告日期：2026-06-24 | 更新：2026-06-24 | 分析范围：ICMS 智慧教室管理系统全栈

---

## 一、已修复缺陷 (30 项)

### 1.1 前端缺陷 (22/22)

| # | 类别 | 问题 | 状态 |
|---|------|------|------|
| 1 | 安全 | Mock 登录不校验密码 | ✅ 已修复 |
| 2 | 缺陷 | 仪表盘统计卡片标签错乱 | ✅ 已修复 |
| 3 | 缺陷 | 修改密码为空操作（无 API 调用） | ✅ 已修复 |
| 4 | 缺陷 | ECharts 实例内存泄漏 (dashboard / environment) | ✅ 已修复 |
| 5 | 缺陷 | 表单 ref 空值崩溃风险 (Login, FreeClassroom, MyReservation) | ✅ 已修复 |
| 6 | 缺陷 | `createWebHistory` 刷新 404 | ✅ 已切换 Hash 模式 |
| 7 | 架构 | `USE_MOCK` / `baseURL` 硬编码 | ✅ 已改为环境变量 |
| 8 | 架构 | ClassroomList / FreeClassroom / User 直接 `import db` | ✅ 已改为 API 调用 |
| 9 | 缺陷 | `handleCardClick` 链式调用 NPE 风险 | ✅ 已修复 |
| 10 | 质量 | 映射工具函数 (getStatusLabel 等) 重复 3+ 次 | ✅ 已抽取 `mappings.js` |
| 11 | 质量 | `timetableGrid` 逻辑 ClassroomList + FreeClassroom 重复 | ✅ 已抽取 `useTimetable.js` |
| 12 | 质量 | ECharts 全量引入 (~1MB) | ✅ 已改为按需引入 |
| 13 | 缺陷 | 全局错误边界缺失 | ✅ 已添加 `app.config.errorHandler` |
| 14 | UX | 环境监测图表暗黑模式颜色不适配 | ✅ 已修复 |
| 15 | UX | Logo 暗黑模式对比度不足 | ✅ 已改为 accent-color |
| 16 | UX | Dropdown item 多余 divided 属性 | ✅ 已修复 |
| 17 | 基础设施 | 无 lint / format 命令 | ✅ 已添加 ESLint 配置 |
| 18 | UX | 操作栏按钮平铺混乱 (3-4 个按钮) | ✅ 已统一为下拉菜单 |
| 19 | UX | 滚动条过细 (6px) / 页面级横向滚动 / 无平滑滚动 | ✅ 已修复 |
| 20 | UX | 暗黑模式切换过渡缺失 | ✅ 已添加 `theme-transitioning` 优化过渡 |
| 21 | 功能 | 通知系统 `is_read` 全用户共享 / 无发送通知功能 | ✅ 已重构为 `read_by[]` 模式 + 通知管理页 |
| 22 | 功能 | 通知列表无轮询 / 已读状态不持久化 | ✅ 已修复 |

### 1.2 后端对接修复 (10/10)

| # | 类别 | 问题 | 状态 |
|---|------|------|------|
| 23 | 缺口 | `GET /api/campuses`、`/api/buildings` 控制器不存在 | ✅ 新建 `CampusBuildingController` |
| 24 | 缺口 | `POST /api/users/{id}/change-pwd` 端点缺失 | ✅ `UserController` 新增 |
| 25 | 缺口 | `PUT /api/notices/{id}/read` + `/read-all` 端点缺失 | ✅ `NoticeController` 重构 |
| 26 | 缺口 | `notice` 表无 `read_by` 字段 | ✅ 实体 + SQL Schema 新增 `read_by JSON` 列 |
| 27 | 不兼容 | 后端返回 camelCase → 前端期望 snake_case | ✅ 新建 `BeanUtils.toMap()` + 7 个 Controller 统一蛇形输出 |
| 28 | 安全 | `SysUser` 实体裸返含 `password` 字段 | ✅ `userToMap()` 去敏处理 |
| 29 | 不兼容 | 工单/预约列表缺 JOIN 名称字段 | ✅ `WorkOrderController` / `ReservationController` 注入名称 |
| 30 | 基础设施 | 手动 SQL 建表无版本控制 | ✅ Flyway 接管：`V1__xxx.sql` + `flyway_schema_history` |
| 31 | 安全 | Spring Security 样板代码 281 行，无角色注解 | ✅ SaToken 替换：`@SaCheckRole` 一行注解鉴权 |
| 32 | 安全 | JWT 明文存 localStorage (XSS 可窃取) | ✅ 双 Token：Access 内存 + Refresh httpOnly Cookie + 轮转 |

---

## 二、致命缺陷 — 生产不可用 (剩余 2 项，已修复 2 项)

> **定义**：不修复则无法安全部署到公网环境。⚠️ 2.1 权限校验 + 2.2 Token 安全已修复。

### 2.1 权限纯前端校验 ✅ 已修复 (SaToken)

| 字段 | 值 |
|------|-----|
| 严重度 | 🟢 已修复 |
| 原状 | 所有 `canEdit` / `canAssign` 仅通过 `userStore.role` 判断；修改 localStorage 即可提权 |
| 修复 | 10 个 Controller 已加 `@SaCheckRole({"admin", "jw"})` / `@SaCheckLogin` 后端硬校验；前端 computed 仅用于 UI 显隐 |

### 2.2 Token 明文存 localStorage ✅ 已修复 (双 Token)

| 字段 | 值 |
|------|-----|
| 严重度 | 🟢 已修复 |
| 原状 | JWT token 存于 `localStorage.getItem('icms_token')`，XSS 一行代码即可窃取 |
| 修复 | Access token (30min) 存 JS 内存变量，不落盘；Refresh token (7天) 存 httpOnly Cookie，JS 不可读；每次 refresh 自动轮转旧 token 立即 DB 失效 |

### 2.3 验证码纯前端生成与校验

| 字段 | 值 |
|------|-----|
| 严重度 | 🟠 高 |
| 现状 | `Login.vue` `refreshCaptcha()` 生成随机字符，前端本地比对 |
| 攻击面 | 抓包后直接对 `/api/auth/login` 发送不存在验证码字段的请求（后端不校验）；脚本批量自动登录 |
| 修复方案 | 验证码由后端生成（图片 / 滑块），登录接口校验 session 中的验证码值 |

### 2.4 登录无限试错（无频率限制）

| 字段 | 值 |
|------|-----|
| 严重度 | 🟠 高 |
| 现状 | Mock 模式无限制；后端 Spring Security 也未配置登录失败锁 |
| 攻击面 | 攻击者可无限制暴力破解任意账户 |
| 修复方案 | 后端：同一 IP/账号 5 分钟失败 5 次锁 30 分钟；前端：连续失败 3 次后强制刷新验证码 |

---

## 三、高风险 — 数据量与性能 (5 项)

> **定义**：数据量超过阈值时系统崩溃或不可用。

### 3.1 所有表格无分页

| 字段 | 值 |
|------|-----|
| 严重度 | 🟠 高 |
| 现状 | ClassroomList / DeviceList / User / MyReservation / Approval / WorkOrderList — 全部调用 `apiGetXxx()` 返回全量数据，`v-loading` 一次性渲染 |
| 影响 | 超过 100 条时首屏加载 >3s；超过 500 条 `el-table` DOM 过重导致明显卡顿 |
| 修复方案 | 引入 Element Plus `el-pagination`，后端 API 支持 `?page=1&size=20`；Mock 模式下前端模拟分页 |

### 3.2 搜索无防抖

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | ClassroomList / DeviceList / User 等页面的 `keyword` / `filters` 绑定了 `v-model`，未做 debounce |
| 影响 | 每敲一个字符就重载全量数据，延迟高时体验极差 |
| 修复方案 | 使用 `useDebounceFn` (VueUse) 或手写 debounce 封装 `loadData` |

### 3.3 ECharts CanvasRenderer 516KB

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | `echarts/renderers` 的 CanvasRenderer 516KB，dashboard 和 environment 页首屏就加载 |
| 影响 | 首页 JS 包 1.2MB，首屏可交互时间 >2s |
| 修复方案 | 改为 `SVGRenderer`（体积 ~80KB）或 dynamic import `() => import('echarts/core')` |

### 3.4 Dashboard 图表是随机假数据

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | `dashboard/index.vue` — `baseRates.map(v => v + Math.floor(Math.random() * 6))` |
| 影响 | 每次刷新显示的数据不同，无法作为管理决策依据 |
| 修复方案 | 数据从后端 `/api/stats/dashboard` 取（已有 endpoint，Mock 关闭后自动走真实数据） |

### 3.5 环境监测数据是 `Math.random()`

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | `api/index.js` `apiGetEnvironmentData` Mock 分支纯随机生成 |
| 影响 | 无法区分真实传感器数据和模拟数据 |
| 修复方案 | 对接后端 IoT 数据源（已有 `/api/environment/records` endpoint，Mock 关闭后自动走真实数据） |

---

## 四、中风险 — 功能缺口 (7 项)

> **定义**：核心业务流程的功能缺失，用户会频繁投诉。

### 4.1 无批量操作

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | 所有表格不提供多选 (`type="selection"`) 功能 |
| 影响 | 管理员需逐条审批预约 / 删除设备 / 指派工单 |
| 修复方案 | 给表格加 `@selection-change` + 顶部工具栏「批量通过」「批量删除」 |

### 4.2 筛选状态不持久化

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | 所有页面的查询条件 (`filters`) 在刷新后全部丢失 |
| 影响 | 管理员在系统日志页筛选一小时后刷新页面全部重置 |
| 修复方案 | 筛选条件同步到 URL query params（`vue-router` 已支持 `useRoute().query`） |

### 4.3 无导出功能

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | 教室 / 设备 / 用户 / 工单 / 日志 / 预约 — 无任何导出按钮 |
| 影响 | 校务汇报、资产盘点无法直接生成 Excel |
| 修复方案 | 引入 `xlsx` (SheetJS) + 导出按钮在各表格顶部 |

### 4.4 通知系统的小缺口

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | 通知面板无「仅未读」筛选；"查看全部通知"底部链接是死链接 |
| 修复方案 | 面板顶部加 `el-radio-group` [全部 / 未读]；底部链接跳到 `/system/notices` |

### 4.5 表单格式校验不全

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | User 新增/编辑表单的 `phone` / `email` 字段无格式校验规则 |
| 修复方案 | 添加 `pattern` / `type: 'email'` 规则 |

### 4.6 课表是伪数据

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | ClassroomList/FreeClassroom 的课表弹窗只展示排课记录二维网格，非真正交互式课表 |
| 修复方案 | 引入专用课表组件或增强 `timetableGrid` 交互（点击单元格建预约等） |

### 4.7 通知已读后历史不可查

| 字段 | 值 |
|------|-----|
| 严重度 | 🟡 中 |
| 现状 | 通知中心半透明显示已读通知，但无独立"已读历史"页面 |
| 修复方案 | `/system/notices` 通知管理页已具备基础功能，尚需增加「已读用户明细」查看入口 |

---

## 五、低风险 — UX 打磨 (5 项)

> **定义**：不阻断业务，但影响用户满意度和专业感。

| # | 问题 | 现状 | 方案 |
|---|------|------|------|
| 1 | 无骨架屏 | Loading 只有 `v-loading` 转圈 | 用 Element Plus `el-skeleton` 替换首屏 loading |
| 2 | 表格无列排序 | 点击表头无反应 | `el-table-column` 加 `sortable="custom"` |
| 3 | 表格 `fixed="right"` 无阴影 | 右侧固定列没有视觉分隔 | 加 `.el-table__fixed-right { box-shadow: -4px 0 8px rgba(0,0,0,0.08); }` |
| 4 | 无"回到顶部"按钮 | 长表格需手动滚动回顶 | 用 `el-backtop` 组件（Element Plus 内置） |
| 5 | 滚动条 hover 色在暗黑模式下不明显 | 当前用 `primary-color` (暗黑=白色)，hover 效果弱 | 暗黑模式下 thumb hover 用 accent-color |

---

## 六、零保障 — 测试与基础设施 (4 项)

> **定义**：没有质量保障手段，无法证明系统可靠性。

| # | 问题 | 现状 | 方案 |
|---|------|------|------|
| 1 | 零单元测试 | 无 `*.test.js`、无 `vitest` 依赖 | 引入 vitest + @vue/test-utils，核心 utils / composables 覆盖率 >80% |
| 2 | 零 E2E 测试 | 无 Cypress / Playwright | 至少覆盖 5 条核心流程（登录→预约→审批→完结） |
| 3 | 无 CI/CD | 无 GitHub Actions / GitLab CI | 添加 `.github/workflows/ci.yml`：lint → test → build |
| 4 | 无错误监控 | 无 Sentry / 自定义埋点 | 接入 `@sentry/vue`，错误边界捕获未处理异常并上报 |

---

## 七、修复优先级建议

### S1 — 上线前必须修（预计 2 天）

```
1. 登录安全：验证码走后端 + 错误限流
2. 表格分页 + 搜索防抖
3. Dashboard 数据从后端取
```

### S2 — 首周迭代（预计 2-3 天）

```
5. 批量操作（审批 / 删除 / 指派）
6. 筛选状态 URL 持久化
7. 导出 Excel
8. 表单 email/phone 校验
9. 骨架屏
```

### S3 — 第二周迭代（预计 2 天）

```
10. 通知面板筛选 + 链接修复
11. 表格排序 + fixed 列阴影
12. 回到顶部
13. 课表交互优化
```

### S4 — 长期基建（持续）

```
14. vitest 单元测试
15. Playwright E2E
16. CI/CD pipeline
17. Sentry 监控
```

---

## 八、已修复项汇总 (33/33)

| 范围 | 已修复 | 状态 |
|------|--------|------|
| 前端缺陷 | 22 项 | ✅ 构建 + Lint 通过 |
| 后端对接缺口 | 9 项 | ✅ Java 编译通过 |
| 安全加固 | 2 项 | ✅ SaToken + 双 Token |
| 基础架构 | 2 项 | ✅ Flyway + 交付分析 |

> 全系统 **0 编译/构建错误**。前端 `npm run dev` 演示。后端需要 MySQL 后 `mvnw.cmd spring-boot:run` 启动。Flyway 管理 DDL，SaToken 管理认证与鉴权 (`@SaCheckRole`)。
