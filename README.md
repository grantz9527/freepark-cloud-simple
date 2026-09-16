# FreePark Cloud Simple

Cloud counterpart of FreePark edge: a parking **cloud platform** for lot configuration, access rules, billing, payments, and edge-node sync.

FreePark 的云端配套：**智慧停车场云平台**，负责车场配置、通行规则、计费支付，以及与场端边缘节点的同步。

## What is it / 项目定位

This repository is the **cloud** side of FreePark. The management console (`frontend-mnt`) and the driver-facing portal (`frontend-user`) talk to a Spring Boot API. Site gates still run on the on-premise edge (`local_server` + `local_frontend`); the cloud pushes lot config over MQTT and receives heartbeats, parking sessions, and gate-open commands.

本仓库是 FreePark 的**云端**：管理后台与用户端对接 Spring Boot API。道闸与识别仍在场端边缘节点闭环；云端通过 MQTT 下发车场配置，并接收心跳、停车流水与开闸指令。

**If you only need on-site access control without cloud billing**, use the edge project by itself. This cloud repo adds multi-lot administration, arrears / occupancy intercept policy, fee quotes, WeChat / Alipay payment, and edge-node management.

**若只需场端通行、暂不需要云端收费**，可单独使用边缘仓库。本云端仓库补充多车场管理、欠费 / 满位拦截策略、算费、微信支付 / 支付宝，以及边缘节点管理。

## Built with AI / 由 AI 驱动构建

This project is an **attempt to be built by AI**: we strive to let AI write the code while humans focus on requirements, design decisions, and review — minimizing hand-written code as much as possible.

本项目**尝试完全由 AI 来构建**：尽力让 AI 完成代码编写，人类只负责提出需求、做设计决策并进行审查，努力做到人工不直接编写代码。

## Vision / 愿景

- Make it easier to find, share, and manage parking spaces across countries and cities.

- 降低找车位、共享车位、管理停车资源的成本，覆盖多国家、多城市场景。

## Goals / 目标

- **I18N first**: language, locale, currency, time zone, and map data should work worldwide.

- **AI-assisted**: use AI to improve search, matching, occupancy prediction, and operations.

- **Open source**: prefer existing open-source components over reinventing the stack.

- **国际化优先**：语言、地区、货币、时区、地图数据面向全球可用。

- **AI 辅助**：用 AI 提升搜索、匹配、占用预测与运营效率。

- **开源优先**：尽量复用成熟开源组件，而不是从零造轮子。

## Stack / 技术栈

The backend uses **Java 21**, Spring Boot 3.5, Spring Data JPA, MySQL, JWT, and HTTP I18N (`Accept-Language` or `?lang=`). Frontends are Vue 3 + TypeScript + Vite.

后端使用 **Java 21**、Spring Boot 3.5、Spring Data JPA、MySQL、JWT，并支持接口国际化。前端为 Vue 3 + TypeScript + Vite。

- [`freepark-cloud-simple-backend`](freepark-cloud-simple-backend): Maven multi-module API, default port `8080`

- [`freepark-cloud-simple-frontend-mnt`](freepark-cloud-simple-frontend-mnt/README.md): admin console, default port `5173`

- [`freepark-cloud-simple-frontend-user`](freepark-cloud-simple-frontend-user/README.md): plate / fee / pay portal, default port `5174`

Backend modules / 后端模块：

| Module | Role |
| --- | --- |
| `startup` | application entry |
| `common` | auth, i18n, shared types |
| `user` | admin login and accounts |
| `parking` | lots, lanes, lists, sessions, intercept, edge sync |
| `billing` | fee rules and special dates |
| `settings` | locale, MQTT, payments |

## Run / 运行

Need **JDK 21**, Node.js 22+, Maven 3.8+, and MySQL 8 on the machine.

本机需要 **JDK 21**、Node.js 22+、Maven 3.8+ 与 MySQL 8。

### Database / 数据库

Create a MySQL schema, then set:

```text
FREEPARK_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/freepark_cloud?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
FREEPARK_MYSQL_UNAME=...
FREEPARK_MYSQL_PWD=...
```

JPA `ddl-auto` is `update`. Do not commit real passwords; this repository is public.

### Backend / 后端

Requires JDK 21.

```bash
cd freepark-cloud-simple-backend
mvn spring-boot:run -pl freepark-cloud-simple-startup -am
```

Or build a jar:

```bash
cd freepark-cloud-simple-backend
mvn -q -DskipTests package
java -jar freepark-cloud-simple-startup/target/freepark-cloud-simple-startup-*.jar
```

API: [http://localhost:8080](http://localhost:8080)

### Admin console / 管理后台

```bash
cd freepark-cloud-simple-frontend-mnt
npm install
npm run dev
```

Console / 控制台：[http://localhost:5173](http://localhost:5173) — Vite proxies `/api` to `http://127.0.0.1:8080`.

### User portal / 用户端

```bash
cd freepark-cloud-simple-frontend-user
npm install
npm run dev
```

Portal / 用户端：[http://localhost:5174](http://localhost:5174)

## Default account / 默认账号

On first startup (when `freepark.user.init.enabled=true`, the default), sign in with:

首次启动后（默认 `freepark.user.init.enabled=true`），使用以下账号登录：

- username / 用户名：`admin`

- password / 密码：`admin123`

Override via `freepark.user.init.username` / `freepark.user.init.password`. **Change this password in production**, or disable init.

可通过配置覆盖。**生产环境请务必修改默认密码**，或关闭初始化。

## Status / 当前状态

FreePark Cloud Simple is **still under active development**. A runnable prototype is in place for multi-lot admin, whitelist / blacklist / pattern allowlist, entry intercepts (arrears, blacklist, full occupancy), parking sessions, billing, payments, and MQTT sync with edge nodes. Features and fixes are landing continuously; no stable release yet.

FreePark Cloud Simple **仍在积极开发中**。目前已具备可运行雏形：多车场管理、白名单 / 黑名单 / 正则名单、入口拦截（欠费、黑名单、满位）、停车流水、计费支付，以及与边缘节点的 MQTT 同步。功能与修复持续更新中，尚未发布稳定版本。

## Support / 支持

If you find this project helpful, please give me a **star**. Your support is my greatest motivation to keep building.

如果觉得本项目对您有帮助，请给我一个 **star**，您的支持是我持续开发的动力。

## Contributing / 参与

Issues and pull requests are welcome.

欢迎提交 Issue 和 Pull Request。

## License / 许可证

Copyright (C) 2026 顾文斌

This project is licensed under the **Apache License 2.0**.

- Full license text: [LICENSE](LICENSE)

- Summary: you may use, modify, and distribute this software, including for commercial purposes; you must retain copyright notices, include the license text, and mark significant changes. See the license for full terms.

本项目采用 **Apache License 2.0** 授权。

- 完整协议文本见 [LICENSE](LICENSE)

- 简要说明：可自由使用、修改和分发，包括商业用途；须保留版权声明、附上许可证文本，并注明重大修改。具体权利与义务以协议全文为准。
