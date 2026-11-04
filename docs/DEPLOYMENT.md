# 部署指南

[← 返回项目首页](../README.md)

采用前后端分离部署：OpenResty / Nginx 提供前端文件，将同一域名的 `/api/` 请求转发到 Spring Boot，后端连接 MySQL。

## 1. 构建产物

在前端目录执行：

```bash
npm ci
npm test
npm run build
```

将 `dist/` **内的文件**上传到网站根目录，使 `index.html` 与 `assets/` 直接位于该目录下。生产环境保持：

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=
```

API 方法已经带有 `/api` 前缀，`VITE_API_BASE_URL` 留空即可使用同源代理。前端环境变量在构建时生效，修改后需要重新构建。

在后端目录执行：

```powershell
.\mvnw.cmd clean package
```

Linux / macOS 使用 `sh mvnw clean package`。上传生成的 `target/icms-backend-1.0.0.jar`，服务器需要 Java 17 和 MySQL 8。

## 2. 后端外部配置

在服务器创建 `application-local.yml`，参考 [配置模板](../ICMS-backend/src/main/resources/application-local.yml.example) 填写数据库连接与随机 JWT 密钥，再按实际域名合并以下字段：

```yaml
app:
  cors:
    allowed-origins: "https://your-domain.example"

server:
  forward-headers-strategy: framework

icms:
  refresh-token:
    secure: true

sa-token:
  is-log: false
```

`secure: true` 用于 HTTPS；仅做 HTTP 测试时设为 `false`。同名顶层节点应合并，不要重复创建 `sa-token` 等节点。

通过绝对路径加载配置，避免面板或容器的工作目录不同导致加载失败：

```bash
java -jar /www/icms/backend/icms-backend-1.0.0.jar \
  --spring.config.additional-location=file:/www/icms/backend/application-local.yml \
  --server.address=127.0.0.1 \
  --server.port=8080
```

路径按实际部署位置调整。上述监听地址适用于代理和 Java 共用宿主机网络的情况；容器部署时，启动命令中的路径必须是容器内路径，并按实际网络调整监听地址和代理目标。

## 3. 配置站点代理

绑定域名、配置网站根目录与 HTTPS 后，在现有 `server` 块内添加或合并：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location = /index.html {
    add_header Cache-Control "no-cache";
}
```

`proxy_pass` 端口后不附加 `/`，以保留 `/api/` 路径。前端使用 Hash 路由，无需额外配置路由回退。

如果 OpenResty 运行在桥接网络容器中，`127.0.0.1` 指向代理容器自身。应使用该容器能访问的后端地址；后端若只监听宿主机回环地址，也需要相应调整。

## 4. 上线检查

- 由面板或服务管理器守护 Java 进程，并配置开机启动。
- 修改初始化账号密码，限制真实配置文件的读取权限，不将配置文件放进网站公开目录。
- 保持后端和数据库仅向必要的内部网络开放，设置数据库定时备份。
- 在真实浏览器验证登录、刷新登录态、退出、预约审批和报修流程。

| 现象 | 优先检查 |
| :--- | :--- |
| 首页 403 | `index.html` 是否位于网站根目录、目录和文件权限是否正确 |
| 接口 502 | 后端进程、端口、容器网络与代理地址 |
| `Invalid CORS request` | 生产 HTTPS 域名白名单、代理头及外部配置是否加载 |
| 接口 404 | `/api/` 是否正确转发、路径前缀是否重复或被移除 |
| 重新登录后仍显示旧界面 | `index.html` 与静态资源是否同步更新、浏览器和 CDN 缓存 |

环境监测接口目前返回模拟指标，尚未接入真实传感器。启用 Sentry 时，前端配置 `VITE_SENTRY_DSN` 后重新构建，后端通过外部 `sentry.*` 配置启用；构建上传凭证只保存在构建环境中。
