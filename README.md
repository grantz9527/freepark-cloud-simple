

# FreePark Cloud Simple (智慧停车场云平台简易版)

## 简介

`FreePark Cloud Simple` 是一个功能完备的智慧停车场云平台管理系统。项目采用前后端分离架构，后端基于 **Spring Boot** 框架构建，前端基于 **Vue 3 + TypeScript + Vite** 框架构建。

系统涵盖了停车场管理、车辆进出控制（黑白名单）、特殊日期计费规则配置、系统参数设置以及管理员账户管理等核心功能，适用于小区、商业停车场等场景。

## 技术栈

### 后端 (Backend)
*   **核心框架:** Spring Boot 3.x
*   **开发语言:** Java 17+
*   **数据持久层:** Spring Data JPA (Hibernate)
*   **安全认证:** JWT (JSON Web Token) + Spring Security
*   **国际化:** Spring i18n (支持英文、中文)
*   **模块化:** Maven 多模块架构

### 前端 (Frontend)
*   **核心框架:** Vue 3
*   **开发语言:** TypeScript
*   **构建工具:** Vite
*   **UI 库:** Element Plus (基于 Vue 3 的主流 UI 库)

## 项目结构

项目采用 Monorepo 风格组织，包含后端服务与两个前端应用：

```text
freepark-cloud-simple/
├── freepark-cloud-simple-backend/              # 后端核心代码
│   ├── freepark-cloud-simple-startup/          # 启动入口模块
│   ├── freepark-cloud-simple-common/           # 公共基础模块 (异常处理、认证拦截、工具类)
│   ├── freepark-cloud-simple-user/             # 用户与权限模块 (管理员登录、账户管理、JWT)
│   ├── freepark-cloud-simple-parking/          # 停车场业务模块 (车位、车道、车辆、Session管理)
│   ├── freepark-cloud-simple-billing/          # 计费模块 (特殊日期规则)
│   └── freepark-cloud-simple-settings/         # 系统配置模块 (时区、语言、车牌颜色)
│
├── freepark-cloud-simple-frontend-mnt/         # 管理后台前端 (Management)
└── freepark-cloud-simple-frontend-user/        # 用户端/访客端前端 (User Portal)
```

## 核心功能特性

### 1. 停车场基础管理
*   **停车场信息:** 支持配置停车场名称、编码、地址、总车位数及地图数据。
*   **区域与车位:** 支持按区域划分车位，支持 Excel 批量导入车位信息。
*   **车道与岗亭:** 灵活配置车道类型（入口、出口、双向）及关联岗亭。

### 2. 智能访问控制
*   **黑白名单:** 管理白名单（VIP/免费）与黑名单（禁止驶入）车辆，支持设置生效时间。
*   **内部车辆:** 管理内部车辆（员工车辆），支持批次录入与删除。
*   **模式名单:** 支持通过正则表达式（Pattern）配置特殊车牌通行规则。
*   **访问判决:** 提供标准 API 接口，根据多维度规则判断车辆是否被拦截。

### 3. 停车记录管理
*   **停车会话:** 记录车辆进出时间、车道、图片等详细信息。
*   **状态追踪:** 实时追踪停车状态（进行中、已完成、已取消）。

### 4. 计费与系统设置
*   **特殊日期:** 配置节假日或特殊时段的计费规则。
*   **系统配置:** 统一管理系统语言、时区、允许使用的车牌颜色等全局参数。

## 快速开始

### 环境依赖
*   JDK 17 或更高版本
*   Node.js 18 或更高版本
*   Maven 3.8 或更高版本
*   MySQL 8.0 或更高版本

### 1. 后端部署

1.  **创建数据库:** 在 MySQL 中创建数据库（例如 `freepark_cloud`）。
2.  **配置数据源:** 编辑 `freepark-cloud-simple-startup/src/main/resources/application.yml`，配置数据库用户名、密码及连接地址。
3.  **构建项目:**
    ```bash
    cd freepark-cloud-simple-backend
    mvn clean install -DskipTests
    ```
4.  **运行服务:**
    ```bash
    java -jar freepark-cloud-simple-startup/target/freepark-cloud-simple-startup-*.jar
    ```

### 2. 前端部署 (管理后台)

1.  **进入目录:**
    ```bash
    cd freepark-cloud-simple-frontend-mnt
    ```
2.  **安装依赖:**
    ```bash
    npm install
    ```
3.  **启动开发服务器:**
    ```bash
    npm run dev
    ```
4.  **构建生产包:**
    ```bash
    npm run build
    ```

## 默认账户

系统启动时，会尝试初始化一个默认的超管账户：

*   **用户名:** `admin`
*   **密码:** `admin123`

> **注意:** 请在首次登录后立即修改默认密码。初始化功能可在 `application.yml` 中通过配置项 (`freepark.user.init.enabled`) 关闭。

## 许可证

本项目遵循开源协议，具体协议信息请参阅项目根目录下的 LICENSE 文件（如有）。