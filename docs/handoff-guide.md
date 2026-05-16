# 项目交接指南

## 1. 项目仓库

GitHub 仓库：

```text
https://github.com/john10hyh/hmdp-local-life-promotion.git
```

推荐克隆命令：

```bash
git clone https://github.com/john10hyh/hmdp-local-life-promotion.git
cd hmdp-local-life-promotion
```

## 2. 接手前必须先读

请按顺序阅读：

1. `README.md`
2. `docs/architecture-design.md`
3. `docs/development-log.md`
4. `docs/handoff-guide.md`
5. `docs/environment-setup.md`
6. `docs/development-workflow.md`

其中：

- `architecture-design.md`：整体架构、模块边界、技术取舍。
- `development-log.md`：阶段状态、TDD 记录、自动化测试结果、人工验收步骤。
- `handoff-guide.md`：跨电脑或跨 AI 接手时的快速上下文。
- `environment-setup.md`：新电脑环境搭建、自检清单和常见问题。
- `development-workflow.md`：SPEC + TDD 工作流、分支和提交规范。

如果实际开发方案与架构文档不一致，必须先更新 `docs/architecture-design.md`，再继续写代码。

每个阶段完成后，必须更新 `docs/development-log.md`，写清楚自动化测试结果、人工测试步骤、预期结果、常见失败原因和是否进入下一阶段。

## 3. 当前项目定位

本项目基于黑马点评 `hmdp` 初始化工程改造，目标是构建一个可运行、可测试、可讲解的 Java 后端面试项目。

目标能力：

1. Redis + Lua 秒杀主链路
2. Kafka 异步下单
3. 订单状态流转、超时关单、乐观锁
4. 商户缓存治理：穿透、击穿、一致性补偿
5. Caffeine + Redis 二级缓存
6. Redis + AOP + 注解限流
7. LangChain4j 智能客服

## 4. 当前阶段状态

已完成：

- 阶段 1A：Redis + Lua 秒杀资格判断
- 阶段 1B：Kafka 异步下单自动化测试

阶段 1B 当前状态：

- 自动化测试通过
- 等待人工验收
- 人工验收步骤已写入 `docs/development-log.md`

下一阶段：

- 阶段 2：订单状态流转、超时关单、乐观锁

在用户明确确认阶段 1B 人工验收通过之前，不要进入阶段 2。

## 5. 当前已实现能力

### 5.1 Redis + Lua 秒杀资格判断

相关文件：

- `src/main/resources/lua/seckill.lua`
- `src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java`
- `src/main/java/com/hmdp/controller/VoucherOrderController.java`
- `src/main/java/com/hmdp/utils/RedisIdWorker.java`

核心 Redis Key：

- 秒杀库存：`seckill:stock:{voucherId}`
- 用户抢购记录：`seckill:order:{voucherId}`

Lua 返回码：

- `0`：成功
- `1`：库存不足
- `2`：不能重复下单

### 5.2 Kafka 异步下单

相关文件：

- `src/main/java/com/hmdp/dto/VoucherOrderMessage.java`
- `src/main/java/com/hmdp/mq/VoucherOrderConsumer.java`
- `src/main/java/com/hmdp/config/KafkaTopicConfig.java`
- `src/main/resources/application.yaml`
- `docker-compose.yml`

Kafka Topic：

```text
voucher-order-create
```

消息字段：

- `orderId`
- `userId`
- `voucherId`
- `createTime`

Consumer 逻辑：

1. 查询 `tb_voucher_order` 是否已有同用户同券订单。
2. 如果已存在，忽略重复消息。
3. 使用 `stock > 0` 条件扣减 `tb_seckill_voucher` 库存。
4. 扣减成功后创建 `tb_voucher_order` 订单，`status=1`，表示未支付。
5. 扣减失败时抛出异常，不创建订单。

数据库兜底：

- `tb_voucher_order` 需要唯一索引：`uk_user_voucher(user_id, voucher_id)`
- 辅助脚本：`docs/stage-1b-voucher-order-unique-index.sql`

## 6. 本地环境与新机器自检

以下环境信息是当前开发机的验证记录，只代表本机已经跑通过。换到另一台电脑后，必须重新验证环境，不要直接假设可用。

当前开发机已验证：

- MySQL：数据库 `hmdp`，账号密码 `root / 1234`
- Redis：`127.0.0.1:6379`
- Maven：可执行 `mvn test`
- Kafka：通过 Docker Compose 运行，不依赖 Windows 原生 Kafka

Kafka 启动：

```bash
docker compose up -d
```

Kafka UI：

```text
http://localhost:8088
```

后端端口：

```text
http://localhost:8081
```

新电脑接手时请逐项自检：

1. Java：确认 IDEA Project SDK 和 Maven 使用 Java 17。
2. Maven：在项目根目录执行 `mvn -q test`，确认依赖能下载、测试能运行。
3. MySQL：确认本机存在 `hmdp` 数据库，账号密码为 `root / 1234`，核心业务表已导入。
4. Redis：确认 `127.0.0.1:6379` 可连接，`PING` 返回 `PONG`。
5. Docker Desktop：确认 Docker 正常启动。
6. Kafka：执行 `docker compose up -d`，确认 `hmdp-kafka` 和 `hmdp-kafka-ui` 容器运行。
7. Kafka UI：浏览器打开 `http://localhost:8088`，能看到 `hmdp-local` 集群。
8. 数据库索引：确认 `tb_voucher_order` 存在 `uk_user_voucher(user_id, voucher_id)`；没有则执行 `docs/stage-1b-voucher-order-unique-index.sql`。
9. 后端启动：运行 `HmDianPingApplication`，确认端口 `8081` 正常。
10. Apifox：确认能调用登录接口拿 token，再调用秒杀接口。

如果任何一项失败，先修环境，不要进入新功能开发。

也可以直接参考完整环境文档：

```text
docs/environment-setup.md
```

## 7. 自动化测试

全量测试：

```bash
mvn -q test
```

阶段 1B 关键测试：

```bash
mvn -q -Dtest=VoucherOrderServiceImplTest#seckillVoucherShouldReturnOrderIdWhenQualified test
mvn -q -Dtest=VoucherOrderConsumerTest#createVoucherOrderShouldDeductMysqlStockAndCreatePendingOrder test
mvn -q "-Dtest=KafkaTopicConfigTest,VoucherOrderConsumerTest" test
```

注意：

- 测试类中通过 `spring.kafka.listener.auto-startup=false` 避免测试启动真实 Kafka Listener。
- 测试中使用 `@MockBean KafkaAdmin` 避免自动化测试强依赖 KafkaAdmin 真实连接。

## 8. 开发原则

必须遵守 TDD：

1. 没有失败测试，不写生产代码。
2. 每个小功能先写测试。
3. 运行测试确认红灯。
4. 实现最小生产代码。
5. 运行测试确认绿灯。
6. 阶段结束后更新 `docs/development-log.md`。

用户只在完整阶段结束后做人工验收，不要每个小功能都打断用户。

## 9. 阶段 1B 人工验收提醒

如果新电脑接手时阶段 1B 还没有人工验收，请先协助用户完成：

1. 启动 Docker Desktop。
2. 执行 `docker compose up -d`。
3. 启动 MySQL、Redis。
4. 在 Navicat 执行 `docs/stage-1b-voucher-order-unique-index.sql`。
5. IDEA 启动 `HmDianPingApplication`。
6. Apifox 登录拿 token。
7. 调用 `POST http://localhost:8081/voucher-order/seckill/{voucherId}`。
8. 检查 Redis、Kafka UI、Navicat。

详细步骤见 `docs/development-log.md` 的“阶段 1B 人工验收步骤”。

## 10. 给新电脑 AI 的接手 Prompt

把下面这段完整复制给另一台电脑上的 AI：

```text
你现在接手一个 Java 后端项目开发任务。项目仓库是：

https://github.com/john10hyh/hmdp-local-life-promotion.git

请先确认你拿到的是 GitHub 最新 main 分支：

git clone https://github.com/john10hyh/hmdp-local-life-promotion.git
cd hmdp-local-life-promotion
git status
git log -1 --oneline

项目是基于黑马点评 hmdp 改造的「跃动一本地生活与大促平台」，目标是按照简历方案 B 实现以下能力：

1. Redis + Lua 秒杀主链路
2. Kafka 异步下单
3. 订单状态流转、超时关单、乐观锁
4. 商户缓存治理：穿透、击穿、一致性补偿
5. Caffeine + Redis 二级缓存
6. Redis + AOP + 注解限流
7. LangChain4j 智能客服

请先克隆仓库并阅读这些文档：

1. README.md
2. docs/architecture-design.md
3. docs/development-log.md
4. docs/handoff-guide.md
5. docs/environment-setup.md
6. docs/development-workflow.md

这些文档是项目交接核心：

- architecture-design.md 记录整体架构、设计思路、模块边界和技术取舍。
- development-log.md 记录开发过程、阶段状态、测试记录和人工验收步骤。
- handoff-guide.md 记录跨电脑接手上下文和当前开发约束。
- environment-setup.md 记录新电脑环境搭建和自检清单。
- development-workflow.md 记录 SPEC + TDD 工作流、分支规范和提交规范。
- 如果实际开发方案和架构设计不一致，必须先更新 architecture-design.md，再继续开发。
- 每个阶段完成后，必须更新 development-log.md，写清楚自动化测试结果、人工测试步骤、预期结果、常见失败原因和是否进入下一阶段。

开发原则：

1. 必须使用 TDD。
2. 没有失败测试，不写生产代码。
3. 每个小功能都要先写测试，运行确认失败，再实现最小代码，再运行确认通过。
4. 用户只在完整阶段结束后做人工验收，不要每个小功能都打断用户。
5. 阶段完成后，要教用户如何人工测试，包括启动哪些服务、调用哪个接口、请求参数是什么、看哪个 Redis Key、看哪个数据库表、正确结果是什么、常见失败原因是什么。
6. 阶段人工验收通过后，再进入下一阶段。

当前环境和项目状态：

- 本仓库已经做过交接前最佳实践升级：包含 `.env.example`、`docker-compose.dev.yml`、GitHub Actions CI、`docs/environment-setup.md`、`docs/development-workflow.md`。
- 新电脑必须先按 `docs/environment-setup.md` 做环境自检；当前开发机的验证记录只能作为参考，不能视为新电脑已经可用。
- MySQL 目标配置：数据库 hmdp，账号密码 root / 1234。新电脑必须重新验证数据库是否存在、表是否导入。
- Redis 目标配置：127.0.0.1:6379。新电脑必须重新验证连接是否可用。
- Kafka 目标配置：使用 docker-compose 在 Docker 中运行，不依赖 Windows 原生 Kafka。新电脑必须重新执行 docker compose up -d 并检查容器状态。
- Kafka UI 目标地址：http://localhost:8088。新电脑必须确认浏览器可打开。
- 后端目标端口：8081。新电脑必须确认 IDEA 能启动应用且端口未被占用。
- 接手后先完成 docs/handoff-guide.md 中“新电脑接手时请逐项自检”的环境检查。
- 阶段 1A：Redis + Lua 秒杀资格判断，已完成并人工验收通过。
- 阶段 1B：Kafka 异步下单，自动化测试已通过，等待人工验收。
- 在用户明确确认阶段 1B 人工验收通过之前，不要进入阶段 2。

当前已实现的关键文件：

- src/main/resources/lua/seckill.lua
- src/main/java/com/hmdp/service/impl/VoucherOrderServiceImpl.java
- src/main/java/com/hmdp/controller/VoucherOrderController.java
- src/main/java/com/hmdp/utils/RedisIdWorker.java
- src/main/java/com/hmdp/dto/VoucherOrderMessage.java
- src/main/java/com/hmdp/mq/VoucherOrderConsumer.java
- src/main/java/com/hmdp/config/KafkaTopicConfig.java
- docker-compose.yml
- docs/stage-1b-voucher-order-unique-index.sql

当前关键测试：

- VoucherServiceImplTest
- SeckillLuaScriptTest
- VoucherOrderServiceImplTest
- VoucherOrderControllerTest
- KafkaTopicConfigTest
- VoucherOrderConsumerTest
- RedisConstantsTest

请接手后先运行：

mvn -q test

如果测试通过：

1. 先协助用户完成阶段 1B 人工验收。
2. 验收通过后，更新 development-log.md。
3. 再进入阶段 2：订单状态流转、超时关单、乐观锁。

如果测试失败：

1. 先分析失败原因。
2. 不要进入新功能。
3. 修复到测试通过为止。

注意：

- 不要跳过测试。
- 不要一次性大改。
- 不要直接开始 Caffeine、限流或 LangChain4j，除非阶段 2、3、4 已按顺序完成。
- Kafka 已经放在阶段 1B；下一阶段重点是订单状态流转。
- 项目最终用于面试，代码实现要能解释清楚为什么 Lua 原子、如何防超卖、如何保证一人一单、Kafka 为什么异步、数据库如何最终兜底。
```
