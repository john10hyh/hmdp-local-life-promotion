# 跃动一本地生活与大促平台

本项目基于黑马点评 `hmdp` 初始化工程改造，目标是沉淀一个可运行、可测试、可讲解的 Java 后端面试项目。当前重点围绕本地生活、优惠券秒杀、异步下单、缓存治理、接口限流和智能客服等高频后端场景展开。

## 已实现能力

- Redis 登录态与验证码缓存，验证码有效期 10 分钟
- Redis + Lua 秒杀资格判断
- 新增秒杀券后同步 Redis 库存
- 秒杀成功返回全局订单号
- Kafka Producer 异步发送下单消息
- Kafka Consumer 异步扣减 MySQL 库存并创建订单
- Consumer 幂等处理重复消息
- MySQL `stock > 0` 条件扣减兜底防超卖
- `user_id + voucher_id` 唯一索引兜底一人一单
- Docker Compose 本地 Kafka 与 Kafka UI

## 技术栈

- Java 17
- Spring Boot 2.7.4
- MyBatis-Plus
- MySQL
- Redis
- Kafka
- Lua
- Docker Compose
- JUnit 5

## 本地启动

1. 启动 MySQL，数据库名为 `hmdp`，账号密码为 `root / 1234`。
2. 启动 Redis，默认地址为 `127.0.0.1:6379`。
3. 启动 Kafka：

```bash
docker compose up -d
```

4. 在 IDEA 中运行 `com.hmdp.HmDianPingApplication`。
5. 后端地址：`http://localhost:8081`。
6. Kafka UI：`http://localhost:8088`。

## 阶段 1B 数据库索引

如果本地数据库还没有一人一单唯一索引，请在 Navicat 执行：

```sql
ALTER TABLE tb_voucher_order
    ADD UNIQUE KEY uk_user_voucher (user_id, voucher_id);
```

脚本文件位于：

```text
docs/stage-1b-voucher-order-unique-index.sql
```

## 自动化测试

```bash
mvn -q test
```

当前已覆盖：

- 新增秒杀券同步 Redis 库存
- Lua 库存不足、重复下单、成功扣库存
- 秒杀接口成功/失败分支
- Kafka 下单消息发送
- Consumer 扣减 MySQL 库存并创建订单
- Consumer 重复消息幂等
- MySQL 库存不足兜底

## 文档

- 架构设计：`docs/architecture-design.md`
- 开发日志：`docs/development-log.md`
- Redis 登录技术报告：`docs/redis-login-technical-report.md`

## 当前阶段

阶段 1A 已完成人工验收。

阶段 1B 已完成自动化测试，等待人工验收。验收通过后进入阶段 2：订单状态流转、超时关单、乐观锁。
