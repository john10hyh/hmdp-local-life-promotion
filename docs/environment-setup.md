# 环境搭建与自检

这份文档用于新电脑接手项目时做环境复现。不要因为另一台机器验证过，就默认本机环境可用。

## 1. 推荐工具

- JDK 17
- IntelliJ IDEA
- Maven
- Docker Desktop
- MySQL / Navicat
- Redis 可视化工具，例如 Another Redis Desktop Manager
- Apifox

## 2. 方式 A：使用本机已有 MySQL / Redis

适合你已经在本机安装并验证过 MySQL、Redis 的情况。

目标配置：

```text
MySQL: 127.0.0.1:3306
Database: hmdp
Username: root
Password: 1234

Redis: 127.0.0.1:6379
Kafka: 127.0.0.1:9092
Kafka UI: http://localhost:8088
```

启动 Kafka：

```bash
docker compose up -d
```

## 3. 方式 B：Docker 一键启动完整依赖

适合新电脑从零搭环境。

```bash
docker compose -f docker-compose.dev.yml up -d
```

包含：

- MySQL 5.7：`hmdp-mysql`
- Redis 7.2：`hmdp-redis`
- Kafka 3.9.1：`hmdp-kafka`
- Kafka UI：`hmdp-kafka-ui`

MySQL 首次启动会自动执行 `hmdp.sql` 初始化表结构和基础数据。

注意：如果本机已经占用了 `3306`、`6379`、`9092` 或 `8088`，Docker 启动会失败。此时要么停掉本机服务，要么调整 compose 端口映射。

## 4. 环境变量

项目默认配置兼容本地开发，也支持环境变量覆盖。

可参考：

```text
.env.example
```

常用变量：

```text
MYSQL_HOST
MYSQL_PORT
MYSQL_DATABASE
MYSQL_USERNAME
MYSQL_PASSWORD
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
SERVER_PORT
```

## 5. 新电脑自检清单

1. `java -version` 能看到 Java 17。
2. IDEA Project SDK 使用 Java 17。
3. `mvn -v` 能看到 Maven 正常。
4. MySQL 中存在 `hmdp` 数据库。
5. Navicat 能连接 MySQL。
6. Redis `PING` 返回 `PONG`。
7. Docker Desktop 正常运行。
8. `docker compose -f docker-compose.dev.yml ps` 能看到依赖容器运行。
9. Kafka UI 能打开：`http://localhost:8088`。
10. IDEA 能启动 `HmDianPingApplication`。
11. Apifox 能调用登录接口拿到 token。
12. `mvn -q test` 能通过。

任何一项失败，都先修环境，不要进入业务开发。

## 6. 常见问题

### 6.1 端口被占用

常见冲突端口：

- `3306`：MySQL
- `6379`：Redis
- `8081`：后端
- `8088`：Kafka UI
- `9092`：Kafka

处理方式：

- 停掉本机已有服务
- 或修改 `docker-compose.dev.yml` 的端口映射
- 或通过环境变量覆盖应用连接地址

### 6.2 数据库唯一索引不存在

执行：

```sql
ALTER TABLE tb_voucher_order
    ADD UNIQUE KEY uk_user_voucher (user_id, voucher_id);
```

脚本位于：

```text
docs/stage-1b-voucher-order-unique-index.sql
```

如果提示索引已存在，可以忽略。

### 6.3 MySQL 连接提示 Public Key Retrieval is not allowed

这是 MySQL 8 默认认证方式下常见的本地开发问题。项目默认 JDBC URL 已带：

```text
allowPublicKeyRetrieval=true
```

如果你在 IDEA 里手动配置了其他运行参数，确认最终连接串仍包含这个参数。

### 6.4 测试启动 Kafka Listener

当前测试类已经通过属性关闭真实 Listener 自动启动，并 Mock 掉 KafkaAdmin。正常执行 `mvn -q test` 不应该依赖真实 Kafka。

### 6.5 中文在 PowerShell 中显示乱码

这是终端编码显示问题，通常不代表文件内容损坏。优先在 IDEA 或 GitHub 页面查看 Markdown 文档。
