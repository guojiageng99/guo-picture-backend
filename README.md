# Guo Picture（果图）

图多多智能协同云图库全栈项目：**后端**为 Spring Boot API，**前端**为 Vue 3 + Vite 单页应用。前后端仓库分离，本地联调时需同时启动两端。

| 部分 | 路径 |
|------|------|
| 后端（本仓库） | 当前目录 |
| 前端 | `E:\code\guo-picture-frontend` |

---

## 后端（guo-picture-backend）

### 技术栈

- **运行时**：Java 11  
- **框架**：Spring Boot 2.7.6  
- **数据访问**：MyBatis-Plus、ShardingSphere JDBC（分库分表）  
- **缓存 / 会话**：Redis、Spring Session、Caffeine  
- **认证**：Sa-Token（与 Session 登录配合，用于部分权限校验）  
- **对象存储**：腾讯云 COS  
- **其他**：WebSocket、Disruptor、Selenium、Jsoup、Knife4j（OpenAPI2）等  

### 默认服务地址

- 端口：**9123**  
- 上下文路径：**`/api`**（例如健康检查类接口形如 `http://localhost:9123/api/...`）  
- 接口文档（Knife4j）：**http://localhost:9123/api/doc.html**

### 环境要求

- JDK 11、Maven 3.6+  
- MySQL（数据库初始化脚本见 `sql/create_table.sql`，默认库名在脚本中为 `yu_picture`，可按实际配置调整）  
- Redis（Session 与 Sa-Token 等依赖 Redis）  
- 腾讯云 COS 账号与存储桶（配置项前缀 `cos.client`，见下文）  
- 若使用爬虫 / 截图等能力，需满足 Selenium 与浏览器环境要求  

### 配置说明

`src/main/resources/application.yml` 中 `spring.profiles.active` 为 **`local`**。本地私密配置应放在 **`application-local.yml`**（该文件已在 `.gitignore` 中忽略，勿提交密钥）。

请在 `application-local.yml` 中至少配置：

- **数据源**：MySQL 连接、ShardingSphere 规则（若启用）  
- **Redis**：主机、端口、密码等  
- **`cos.client`**：`host`、`secretId`、`secretKey`、`region`、`bucket`  

可参考同目录下 `application.yml` 的公共项（如 MyBatis-Plus、Knife4j、上传大小限制等）。

### 初始化数据库

在 MySQL 中执行：

```bash
# 在 sql 目录下按你的客户端执行，例如：
mysql -u root -p < sql/create_table.sql
```

根据实际库名、账号修改脚本或配置中的 JDBC URL。

### 构建与运行

```bash
mvn clean package -DskipTests
java -jar target/guo-picture-backend-0.0.1-SNAPSHOT.jar
```

开发时可直接在 IDE 运行主类：

`com.guo.guopicturebackend.GuoPictureBackendApplication`

或使用：

```bash
mvn spring-boot:run
```

### 主要接口模块（Controller）

包路径：`com.guo.guopicturebackend.controller`

- `UserController` — 用户注册、登录、会话等  
- `PictureController` — 图片相关  
- `SpaceController` / `SpaceUserController` / `SpaceAnalyzeController` — 空间与成员、分析  
- `FileController` — 文件上传等  
- `MainController` — 其他入口  

完整路径与参数以 **Knife4j** 文档为准。

---

## 前端（guo-picture-frontend）

### 技术栈

- **Vue 3** + **TypeScript** + **Vite 5**  
- **Ant Design Vue 4**、**Pinia**、**Vue Router 4**  
- **Axios**、**ECharts**（含词云）、**vue-cropper** 等  

### 环境要求

- Node.js（建议与 `package.json` 中 dev 依赖匹配的 LTS 版本，如 20/22）  
- npm 或兼容包管理器  

### 安装与脚本

在 **`E:\code\guo-picture-frontend`** 目录下：

```bash
cd E:\code\guo-picture-frontend
npm install
```

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器（默认 **5173**） |
| `npm run build` | 类型检查 + 生产构建 |
| `npm run preview` | 预览构建产物 |
| `npm run openapi` | 根据 OpenAPI 配置生成前端 API 代码（见 `openapi.config.js`） |
| `npm run lint` | ESLint 检查并自动修复 |
| `npm run format` | Prettier 格式化 |

### 与后端联调

开发环境下，Vite 已将 **`/api`** 代理到 **`http://localhost:9123`**，并开启 **WebSocket** 代理。请先启动后端（端口 9123），再执行 `npm run dev`，前端通过同源路径 `/api/...` 访问后端即可。

### 推荐 IDE

VS Code + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)（勿与 Vetur 同时用于 Vue 3）。

---

## 联调小结

1. 配置后端 `application-local.yml`（MySQL、Redis、COS 等）并执行 `sql/create_table.sql`。  
2. 启动后端：`mvn spring-boot:run` 或运行主类，确认 **http://localhost:9123/api/doc.html** 可访问。  
3. 在前端目录执行 `npm install` 与 `npm run dev`，浏览器访问 **http://localhost:5173**（以控制台输出为准）。

---

## 许可证

若需对外开源，请在本仓库补充 `LICENSE` 并在此 README 中注明。
