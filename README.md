# AI 旅游规划助手

Spring Boot + Vue 3 全栈项目，基于大语言模型生成个性化旅游行程规划。

## 功能

- **AI 行程生成** — 根据城市、预算、天数生成详细行程（SSE 流式输出，边生成边显示）
- **用户系统** — 注册 / 登录 / JWT 鉴权 / 个人资料
- **行程管理** — 保存行程 / 查看历史行程 / 编辑行程
- **景点收藏** — 收藏喜欢的景点，查看收藏列表
- **天气查询** — 显示目的地 3 天天气预报
- **景点地图** — 高德地图标注景点位置，支持按天数/时段筛选
- **景点导航** — 一键跳转高德地图 APP/网页版导航
- **AI 对话** — 旅游相关问题智能问答
- **行程分享** — 生成行程图片，可下载或分享

## 技术栈

### 后端
- Spring Boot 4.x
- Spring Data JPA + Hibernate
- MySQL
- OkHttp（调用 LLM API）
- JWT 鉴权
- SSE（Server-Sent Events）流式输出

### 前端
- Vue 3 + Composition API
- Vant 4（移动端 UI 组件库）
- Vite（构建工具）
- Pinia（状态管理）
- Leaflet（地图）
- html2canvas（行程分享图）

### 第三方服务
- **SiliconFlow** — LLM 推理（GLM-5.1）
- **Open-Meteo** — 天气数据（免费，无需 Key）
- **高德开放平台** — 地理编码 / 地图瓦片 / 导航

## 项目结构

```
project2/
├── travel-project1/              # 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/example/travel_project1/
│       │   ├── controller/       # 接口层
│       │   ├── service/          # 业务层
│       │   ├── repository/       # 数据访问层
│       │   ├── entity/           # 实体类
│       │   ├── dto/              # 请求参数对象
│       │   ├── vo/               # 返回值对象
│       │   ├── common/           # 配置 / 过滤器 / 工具
│       │   └── utils/            # LLM 调用工具
│       └── resources/
│           ├── application.yml           # 配置（密钥从环境变量读取）
│           └── application.example.yml   # 配置模板
└── travl-1/                      # 前端
    ├── package.json
    ├── .env.development          # 开发环境 API 地址
    ├── .env.production           # 生产环境 API 地址
    └── src/
        ├── views/                # 页面
        ├── components/           # 组件
        ├── router/               # 路由
        ├── stores/               # Pinia
        └── utils/                # 请求封装
```

## 快速开始

### 1. 准备工作

- JDK 17+
- Node.js 18+
- MySQL 8+
- 注册 [SiliconFlow](https://cloud.siliconflow.cn) 获取 API Key
- 注册 [高德开放平台](https://open.amap.com) 获取 Web服务 Key

### 2. 配置后端

```bash
cd travel-project1

# 复制配置模板
cp src/main/resources/application.example.yml src/main/resources/application.yml

# 编辑 application.yml，填入你的密钥
# 或者通过环境变量配置：
export LLM_API_KEY=sk-你的key
export DB_PASSWORD=你的mysql密码
export JWT_SECRET=你的jwt密钥
export AMAP_KEY=你的高德key
```

### 3. 启动后端

```bash
cd travel-project1
mvn spring-boot:run
# 后端运行在 http://localhost:3200
```

### 4. 启动前端

```bash
cd travl-1
npm install
npm run dev
# 前端运行在 http://localhost:5173
```

### 5. 数据库

MySQL 建库：
```sql
CREATE DATABASE travel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

JPA 配置 `ddl-auto: update`，首次启动会自动建表。

## 主要接口

| 接口 | 说明 |
|---|---|
| POST /api/auth/register | 注册 |
| POST /api/auth/login | 登录 |
| POST /api/travel/recommend/stream | AI 行程推荐（SSE 流式） |
| POST /api/plans | 保存行程 |
| GET /api/plans | 我的行程列表 |
| GET /api/weather | 天气查询 |
| GET /api/geo/search | 高德地理编码代理 |
| POST /api/favorites | 收藏景点 |
| GET /api/favorites | 收藏列表 |
| POST /api/chat | AI 对话 |

## License

MIT
