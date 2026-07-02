# ICMS 智慧教室管理系统 — 答辩与代码理解手册

本手册专门针对 ICMS（Intelligent Classroom Management System）项目的技术实现与答辩防抽查而编写。内容紧密围绕您工作区中的真实代码，梳理了老师最喜欢抽查的核心逻辑、安全机制、数据库设计以及前端优化点，并提供了**标准答辩话术**。

---

## 🧭 一、 全栈技术栈与目录架构

老师可能会问：*“简述一下你的项目结构，前端和后端的业务逻辑分别写在哪里？”*

### 1.1 技术选型一览
* **前端**：Vue 3 (Composition API) + Vite (构建) + Pinia (状态) + Element Plus (UI) + ECharts (可视化) + Axios (请求)。
* **后端**：Spring Boot 3.2.4 + MyBatis-Plus (ORM) + SaToken (权限认证) + Log4j2 (日志)。
* **数据层**：MySQL 8.x + Flyway (版本迁移控制)。

### 1.2 核心代码目录定位
在被要求“现场找文件/找代码”时，请对照下表快速定位：

| 模块位置 | 文件路径 (以项目根目录为起点) | 作用与核心逻辑描述 |
| :--- | :--- | :--- |
| **前端路由与守卫** | `ICMS-frontend/src/main.js` & `router/index.js` | 路由守卫 `beforeEach`，处理未登录拦截、路由鉴权、自动刷新 Token |
| **前端状态管理** | `ICMS-frontend/src/store/user.js` | Pinia 用户 store，管理 Token 内存存储、日间/暗黑主题平滑切换 |
| **前端 API 请求层** | `ICMS-frontend/src/api/index.js` | 支持 Mock/API 双通道拦截；Axios 拦截器处理并发 401 自动无缝刷新 Token |
| **本地 Mock 数据库** | `ICMS-frontend/src/db/mockDb.js` | 纯前端运行的核心，用 LocalStorage 模拟了关系型数据库增删改查 |
| **后端入口类** | `ICMS-backend/.../IcmsApplication.java` | Spring Boot 启动类，包含 Flyway 自动执行和数据初始化逻辑 |
| **后端安全拦截配置** | `ICMS-backend/.../security/SaTokenConfig.java` | 配置 SaToken 路由拦截器与跨域过滤器（CorsFilter） |
| **后端鉴权映射实现** | `ICMS-backend/.../security/StpInterfaceImpl.java` | 继承 `StpInterface`，用于向 SaToken 注入当前用户的角色与权限列表 |
| **后端登录限制器** | `ICMS-backend/.../security/LoginLimiter.java` | 用于同 IP + 账号登录错误计数，触发封锁限制，防暴力破解 |
| **后端验证码工具** | `ICMS-backend/.../security/CaptchaUtil.java` | 绘制 4 位包含背景干扰线和噪点的彩色 SVG 验证码并进行服务端校验 |
| **后端 AOP 日志切面** | `ICMS-backend/.../common/aspect/LogAspect.java` | AOP 环绕通知切面，捕获 `@Log` 注解自动序列化参数，异步入库 |
| **后端命名转换工具** | `ICMS-backend/.../common/BeanUtils.java` | 通过反射将 Java 实体的 camelCase 属性转换为前端/数据库 snake_case |
| **核心业务控制器** | `ICMS-backend/.../controller/` | 包含 Auth（登录）、Classroom（教室）、Reservation（预约）、WorkOrder（工单）等 11 个控制器 |

---

## 🧠 二、 核心业务逻辑设计（老师抽查重灾区）

### 2.1 教室预约“冲突检测”模型（重要！）
老师必问：*“两个预约时间重叠怎么办？你的预约冲突和排课冲突是怎么检测的？”*

#### 1. 核心数学模型（区间相交判定）
在判断新预约区间 `[newStart, newEnd]` 是否与已有区间 `[existStart, existEnd]` 冲突时，系统采用**区间相交**算法。
只要满足以下条件，即代表时间段重叠：
$$\text{new\_start} \le \text{exist\_end} \quad \text{AND} \quad \text{new\_end} \ge \text{exist\_start}$$

#### 2. 后端代码实现
逻辑写在 [ClassroomServiceImpl.java](file:///E:/%E6%A1%8C%E9%9D%A2/ICMS/ICMS-backend/src/main/java/com/icms/service/impl/ClassroomServiceImpl.java#L68-L94) 的 `checkConflict` 方法中：
```java
public boolean checkConflict(Long classroomId, LocalDate date, Integer startSection, Integer endSection) {
    // 1. 检查排课占用（根据周几 weekday 进行匹配，状态必须为 published）
    int weekday = date.getDayOfWeek().getValue(); // 1-7 代表周一到周日
    Long scheduleCount = courseScheduleMapper.selectCount(new LambdaQueryWrapper<CourseSchedule>()
            .eq(CourseSchedule::getClassroomId, classroomId)
            .eq(CourseSchedule::getWeekday, weekday)
            .le(CourseSchedule::getStartSection, endSection) // 核心相交条件
            .ge(CourseSchedule::getEndSection, startSection)
            .eq(CourseSchedule::getStatus, "published"));
    if (scheduleCount > 0) return true;

    // 2. 检查已审批通过的预约占用（reservationDate 匹配，状态为 approved）
    Long reservationCount = classroomReservationMapper.selectCount(new LambdaQueryWrapper<ClassroomReservation>()
            .eq(ClassroomReservation::getClassroomId, classroomId)
            .eq(ClassroomReservation::getReservationDate, date)
            .eq(ClassroomReservation::getStatus, "approved")
            .le(ClassroomReservation::getStartSection, endSection) // 核心相交条件
            .ge(ClassroomReservation::getEndSection, startSection));

    return reservationCount > 0;
}
```

### 2.2 审批时的并发防抢占机制
老师可能会问：*“如果有两个教务老师在不同电脑上同时审批通过同一间教室的冲突预约，怎么防止冲突？”*
* **代码答复**：在 [ClassroomReservationServiceImpl.java](file:///E:/%E6%A1%8C%E9%9D%A2/ICMS/ICMS-backend/src/main/java/com/icms/service/impl/ClassroomReservationServiceImpl.java#L72-L87) 的 `approveReservation` 中，当审批动作传入为 `"approved"` 时，系统在事务内会**重新校验一次冲突**（Double Check）。如果检测到冲突（比如另一笔预约刚刚被同意了），会**自动将当前预约强制更新为 `rejected`（发生冲突：该教室已被抢占）** 并抛出异常回滚，防止并发冲突的发生。

### 2.3 AOP 审计日志实现原理
老师可能会问：*“操作日志是怎么记录的？难道每个接口里都要写一遍保存日志的代码吗？”*
* **实现方案**：系统基于 **AOP（面向切面编程）** 技术，声明了自定义注解 `@Log`。
* **核心切面类**：[LogAspect.java](file:///E:/%E6%A1%8C%E9%9D%A2/ICMS/ICMS-backend/src/main/java/com/icms/common/aspect/LogAspect.java)。
* **逻辑过程**：使用环绕通知 `@Around("@annotation(controllerLog)")`，在接口执行前后拦截：
  1. 通过 `RequestContextHolder` 拿到 `HttpServletRequest`，解析出请求 URL、Method 和客户端 IP。
  2. 利用 Jackson 的 `objectMapper` 自动将请求的第一个入参序列化为 JSON 字符串，保存为请求参数。
  3. 通过 SaToken 的 `StpUtil` 拿到当前登录用户的 ID 与用户名。
  4. 执行 `joinPoint.proceed()`，成功则设置状态为 `success`，抛异常则捕获并将异常信息（`e.getMessage()`）记入错误字段，最终将日志**异步保存**至数据库 `operation_log` 表中。

---

## 🛡️ 三、 安全防护体系（答辩绝对加分项）

老师非常看重系统安全性，本系统在安全加固上做了大量的专门设计：

### 3.1 接口越权防护 (RBAC)
* **实现**：采用 SaToken 轻量级权限框架。
* **防止前端越权绕过**：仅在前端隐藏按钮是不安全的，攻击者可以通过 Postman 强行调用接口。因此，在后端 Controller 层的敏感操作方法上，统一加上了 `@SaCheckRole(value = {"admin", "jw"}, mode = SaMode.OR)` 注解进行强校验（如教室新增修改、预约审批、用户管理等）。

### 3.2 令牌防盗与无损刷新（双 Token 机制）
老师可能会问：*“你的 JWT 存在哪里？怎么防止 XSS 攻击窃取 Token？”*
* **安全痛点**：如果把 Token 存在浏览器的 LocalStorage 中，一旦页面遭遇 XSS 跨站脚本攻击，黑客可以通过 `localStorage.getItem` 轻松窃取 Token。
* **解决方案（双 Token）**：
  1. **Access Token (短期访问令牌)**：有效期 30 分钟，**只存在前端 JavaScript 内存中**，不落盘（不存 Cookie 也不存 LocalStorage），JS 变量在页面刷新后即消失，XSS 无法通过常规手段持久化窃取。
  2. **Refresh Token (长期刷新令牌)**：有效期 7 天，**存存放于 httpOnly Cookie 中**。由于设置了 `httpOnly=true`，前端 JS 完全无法读取此 Cookie。
* **无缝刷新（Silent Refresh）流程**：
  * 当页面刷新或内存 Token 到期时，前端 Axios 响应拦截器拦截到 `401 Unauthorized` 错误。
  * 拦截器会**自动暂停后续请求**，并向后端发送一个异步刷新请求 `POST /api/auth/refresh`。此时浏览器会自动携带 httpOnly 中的 Refresh Token。
  * 后端校验 Refresh Token 有效后，重新调用 `StpUtil.login()` 并在响应体中返回一个新的 Access Token，同时**轮转（Rotate）**刷新 Cookie。
  * 前端拿到新 Access Token 后，更新内存变量，并**无感重试刚才失败的请求**，用户对此完全没有感知。

### 3.3 暴力破解与重放攻击防护
* **SVG 彩色干扰验证码**：登录接口引入 `CaptchaUtil.java`。后端生成 4 位随机字符，并渲染为包含随机彩色噪点和干扰线的 SVG 矢量图，将验证码字符放入后端 Session/缓存（或一次性验证码 ID）中。验证码校验是一次性的，比对后立刻销毁，防止机器识别和重放攻击。
* **同 IP 登录限流机制**：[LoginLimiter.java](file:///E:/%E6%A1%8C%E9%9D%A2/ICMS/ICMS-backend/src/main/java/com/icms/security/LoginLimiter.java) 基于内存计数。当同一个 IP + 账号在 5 分钟内连续登录失败 5 次，系统自动锁定该 IP 与账号的登录请求 15 分钟，有效抵御针对管理员账号的密码爆破。

---

## 🗃️ 四、 数据库设计与 Flyway 版本控制

### 4.1 核心数据表与关联关系
* **RBAC 权限体系**：`sys_user`（用户） ➔ `sys_user_role`（多对多关联） ➔ `sys_role` ➔ `sys_role_permission` ➔ `sys_permission`（菜单与接口控制）。
* **资源层级**：`campus`（校区） ──< `building`（楼栋） ──< `classroom`（教室） ──< `device`（设备）。
* **业务关联**：所有业务表（`classroom_reservation` 预约表、`work_order` 工单表、`environment_record` 环境监测表）均通过 `classroom_id` 物理或逻辑外键与 `classroom` 关联，并关联 `sys_user` 的用户 ID。

### 4.2 数据库版本迁移控制 (Flyway)
老师可能会问：*“当你的项目部署到新服务器上时，你怎么保证别人数据库里的表结构和你开发时一模一样？难道是手动导出 SQL 文件导入吗？”*
* **标准回答**：本系统放弃了落后的“手动导入 SQL”方式，引入了 **Flyway 数据库迁移框架**。
* **工作原理**：
  * 迁移脚本存放在 `src/main/resources/db/migration` 下，按照版本号命名，例如 `V1__init_schema.sql`、`V2__add_refresh_token.sql`。
  * 项目启动时，Spring Boot 自动激活 Flyway。Flyway 会在数据库中建立一张名为 `flyway_schema_history` 的元数据表。
  * Flyway 自动对比本地脚本与数据库中已执行脚本的哈希值与版本号，**自动按顺序增量执行**未运行的 SQL 脚本。
  * 从而实现了数据库结构的版本化控制，确保了“代码到哪里，数据库表结构就自动演进到哪里”。

---

## ⚡ 五、 前端交互与性能优化细节

老师可能会问：*“你在前端开发中遇到了什么性能问题吗？是怎么解决的？”*

1. **ECharts 首屏按需加载与 SVG 渲染**：
   * **问题**：全量引入 ECharts 会导致前端打包后的 vendor.js 增加近 1MB，严重拖慢首屏白屏时间。而且默认使用 Canvas 渲染器，在配置一般的展示终端上有些许卡顿。
   * **优化**：在代码中改用**按需引入**（只导入 `LineChart`、`BarChart`、`PieChart` 以及 `GridComponent` 等必须组件），并将渲染器替换为更轻量的 `SVGRenderer`，使得打包体积减小了约 500KB，首屏加载效率提升明显。
2. **防抖搜索处理**：
   * 在用户在输入框键入搜索条件时，如果每打一个字就发送一次后端请求，会给后台数据库造成极大的查询压力。前端利用 lodash/自定义防抖函数封装了 `debouncedKeyword`，设定 **300ms 延迟防抖**，只有在用户停止输入 300ms 后才自动发起分页查询。
3. **日夜主题平滑切换**：
   * 采用全局 CSS 变量增加颜色体系（CSS Custom Properties）。切换暗黑模式时，给 `html` 根节点添加 `theme-transitioning` 类，在 CSS 中对颜色变化设置平滑的 `transition: background-color 0.4s ease` 动画，提供了极其高端的视觉平滑过渡体验。

---

## 💬 六、 答辩抽查高频 Q&A 话术库

### Q1：你的系统是如何防止 SQL 注入的？
> **答**：系统在持久层采用了 MyBatis-Plus。在 SQL 映射中，我们严格遵循参数化查询的原则，所有的参数绑定均使用 **`#{param}`** 占位符，而不是 `${param}` 拼接。MyBatis 会把 `#{}` 编译为 JDBC 的 `PrepareStatement` 参数占位符 `?`，在执行前由数据库驱动进行安全预编译，从而从底层彻底杜绝了 SQL 注入攻击。

### Q2：如果两个人在同一毫秒点击预约同一个教室，系统会发生什么？怎么解决并发冲突的？
> **答**：
> 1. 首先，我们在数据库设计上，对于“审批通过”的预约做了事务和重校验控制。
> 2. 在业务逻辑中，我们对整个审批逻辑打上了 **`@Transactional` 事务注解**，保证其原子性。
> 3. 当两个审批请求并发到达时，即使它们第一步“检查冲突”都通过了，在最终更新状态的写入阶段，后端在事务内会进行 **Double Check（重新校验冲突）**。
> 4. 先提交的事务会修改数据并提交，后提交的事务在重新校验时会发现该时间段已被刚刚提交的事务所抢占，从而抛出自定义异常 `CustomException` 并自动回滚，将状态置为 `rejected`（发生冲突：该教室已被抢占），这有效防止了并发冲突问题。
> 5. 在更高并发的生产环境下，我们还可以在数据库层面对冲突时段加上行级锁（`SELECT ... FOR UPDATE`）或利用 Redis 分布式锁来保证绝对的排他性。

### Q3：你的前后端接口中，Java 命名是驼峰（camelCase），但前端和数据库需要下划线（snake_case），你是怎么做命名转换的？
> **答**：我们采取了双重保障机制：
> 1. **在数据库与后端实体转换时**：在 MyBatis-Plus 配置中，开启了 `map-underscore-to-camel-case: true`，这样 MyBatis 会自动将数据库字段（如 `created_at`）映射为 Java 实体的驼峰字段（如 `createdAt`）。
> 2. **在后端返回给前端 JSON 时**：对于通过 Map 返回的自定义结构，我们编写了一个反射工具类 `BeanUtils.toMap(Object obj)`。它会通过反射获取 Java 对象的所有声明字段，自动调用 `camelToSnake` 转换算法，将字段名转换为蛇形下划线格式（如 `user_type`），再输出给前端，从而保证了前端命名规范的高度一致。

### Q4：用户管理中的密码是如何存储的？如果数据库泄露了，黑客能看到用户的明文密码吗？
> **答**：完全不能。系统坚决杜绝明文存储密码，也未使用简单的 MD5 加密（MD5 容易通过彩虹表暴力破解）。我们采用了 **BCrypt 强哈希加密算法**。BCrypt 会在加密时自动引入随机“盐（Salt）”进行混合哈希，这意味着即使两个用户的原始密码一模一样，加密后的密文也是完全不同的。BCrypt 的单向哈希不可逆，即使数据库泄露，黑客也无法通过密文反推出明文密码，极大地保障了用户的信息安全。

---

## 2026-07-02 安全加固答辩补充

### 后端权限边界怎么保证不是只靠前端？

后端所有敏感业务都以 SaToken 当前登录态为准，不信任请求体中的用户 ID。预约列表中，`admin`/`jw` 可以查全量，普通用户即使传入他人的 `applicant_id`，后端也会强制改为 `StpUtil.getLoginIdAsLong()`；工单列表中，`repairer` 只能看到 `handlerId` 为自己的工单，其他普通用户只能看到自己提交的工单。

### 工单处理如何防止越权和乱改状态？

`POST /api/work-orders/{id}/handle` 只接受 `processing`、`finished` 两个状态。服务层会校验当前用户：管理员和教务可代处理，维修人员必须是该工单的 `handlerId`。状态流转也有限制：只能从 `assigned` 进入 `processing`，或从 `assigned/processing` 进入 `finished`，不能从 `submitted`、`closed`、`finished` 等状态随意跳转。

### 登录限流覆盖哪些失败？

登录失败现在分两类都会计数：验证码错误，以及账号/密码校验失败。失败计数使用 `LoginLimiter.buildKey(username, ip)` 形成 IP + 账号维度的 key；只有验证码、密码都校验通过并完成登录后，才会清除失败计数。

### 用户管理为什么不用完整实体直接保存？

用户保存接口已经改为白名单字段写入，只允许更新用户名、姓名、邮箱、手机号、角色、状态等业务字段。请求体中的 `password`、`refreshToken`、`deleted`、`createdAt` 等敏感字段会被忽略。密码变更必须走“修改密码”或“重置密码”接口，并统一使用 BCrypt 哈希。

### 并发审批现在如何处理？

预约审批只允许 `approved` 或 `rejected`。审批服务会先确认记录仍为 `pending`，通过审批前重新做冲突检测，最终更新时还会带上 `status = pending` 条件。如果记录已被其他请求处理，更新失败并返回“预约状态已变化，请刷新后重试”，避免重复审批或双通过。

### 本轮验证结果

```bash
cd ICMS-backend
mvnw.cmd test
```

结果：`BUILD SUCCESS`，新增 5 个后端单元测试全部通过，覆盖用户保存白名单、工单处理人校验和工单状态流转。
