智能电商微服务秒杀系统

基于 Java 21 + Spring Boot 3 + Spring Cloud Alibaba + Redis + MySQL 开发的微服务电商秒杀系统，实现用户认证、商品管理、库存管理、秒杀下单和订单查询等核心功能。

项目重点实践高并发库存处理、重复下单控制、接口限流、服务治理等后端技术，并接入大模型 API 扩展基础 AI 风险评估功能。

✨ 项目亮点

* 基于 Spring Cloud Alibaba 实现微服务拆分
* 使用 Nacos 实现服务注册、发现与配置管理
* 使用 Spring Cloud Gateway + JWT 实现统一网关与身份认证
* 使用 Redis + Lua 实现库存原子扣减
* 使用 Redisson 分布式锁 + 幂等 Token 控制重复下单
* 使用数据库唯一约束增强订单幂等性
* 使用 Sentinel 实现秒杀接口限流与系统保护
* 使用 Docker 搭建 MySQL、Redis 等运行环境
* 接入 LLM API 实现基础 AI 风险评估

🛠️ 技术栈

后端：

Java 21 Spring Boot 3 Spring Cloud Alibaba Nacos Gateway Sentinel MyBatis-Plus JWT

中间件：

Redis Lua Redisson MySQL 8 Docker

前端：

Vue 3 Vite

AI：

LLM API

🔥 核心实现

1. 高并发库存处理

将秒杀库存缓存至 Redis，通过 Lua 脚本将库存判断与扣减合并为原子操作，减少并发请求下的库存竞争以及数据库压力。

请求 → Redis → Lua 原子扣减 → 业务校验 → MySQL

2. 防止重复下单

使用 幂等 Token + Redisson 分布式锁 + 数据库唯一约束，从请求、业务和数据库多个层面控制重复订单。

请求
 ↓
幂等校验
 ↓
分布式锁
 ↓
订单校验
 ↓
创建订单
 ↓
数据库唯一约束

3. 秒杀流量控制

使用 Sentinel 对秒杀核心接口进行 QPS 限流和系统保护，降低突发流量对服务的影响。

4. 微服务治理

通过 Nacos 管理服务注册、发现和配置，Gateway 统一处理请求路由与 JWT 鉴权，各业务服务独立负责对应领域逻辑。

5. AI 风险评估

接入大模型 API，根据用户请求行为进行基础风险分析，为秒杀业务增加 AI 风险评估能力。

📂 项目结构

flash-sale-system/
├── gateway-service/          # 网关服务
├── auth-service/             # 用户认证
├── product-service/          # 商品服务
├── inventory-service/        # 库存服务
├── flash-sale-service/       # 秒杀服务
├── order-service/            # 订单服务
├── flash-sale-frontend/      # Vue 3 前端
├── sql/                      # 数据库脚本
├── docker-compose.yml        # Docker 配置
└── README.md

⚙️ 本地运行

环境要求

JDK 21+
Maven 3.9+
Node.js 18+
MySQL 8+
Redis 7+
Docker

启动基础环境

docker compose up -d

初始化数据库后，启动 Nacos 以及各微服务。

启动前端

cd flash-sale-frontend
npm install
npm run dev

🎯 项目实践

通过本项目主要实践：

* Java 后端与微服务开发
* Redis 高并发场景应用
* Lua 原子操作
* Redisson 分布式锁
* Sentinel 限流
* JWT 鉴权
* MySQL 数据持久化
* Docker 环境部署
* LLM API 接入

👨‍💻 About

本项目用于 Java 后端学习与技术实践。

GitHub：@seventh7472

📄 License

本项目仅用于学习与技术交流。
