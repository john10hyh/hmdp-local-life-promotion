# 跃动一本地生活与大促平台开发过程记录

## 1. 文档目标

本文档用于记录项目开发过程、阶段进度、测试结果、人工验收状态和交接信息。任何 AI 或开发者接手时，应先阅读本文档，再阅读 `docs/architecture-design.md` 和当前代码。

## 2. 协作约定

开发方式：

- 采用 TDD：先写失败测试，再实现功能，再跑通测试。
- 自动化测试由开发窗口持续执行。
- 人工测试按完整阶段进行，不要求用户每个小功能都手测。
- 阶段未通过人工验收前，不进入下一个阶段。

文档维护：

- 架构与实际代码不一致时，先更新 `docs/architecture-design.md`。
- 每个阶段完成后，更新本文档中的进度、测试结果和交接说明。
- 每次人工测试前，必须在本文档写清楚测试入口、测试数据、预期结果和排错方向。

## 3. 当前环境信息

- 项目目录：`/mnt/d/hmdp-init`
- 后端框架：Spring Boot 2.7.4
- Java 版本：8
- 数据库：MySQL
- 缓存：Redis
- ORM：MyBatis-Plus
- 当前日期：2026-05-10

环境验证记录：

- MySQL：已验证通过，`hmdp` 数据库和核心业务表存在。
- Redis：已验证通过，`PING`、`SET`、`GET`、`DEL` 均正常。
- Maven：已验证通过，`mvn test` 执行成功。
- Kafka：Windows 原生 Kafka 未安装；当前采用 Docker Compose 提供本地 Kafka，容器名 `hmdp-kafka`，宿主机访问地址 `127.0.0.1:9092`，Kafka UI 地址 `http://localhost:8088`。

## 4. 当前代码基线

已确认能力：

- Redis 短信验证码登录
- Token 登录态存储
- 登录态续期拦截器
- 登录校验拦截器
- 基础商户、优惠券、用户 Controller
- 阶段 1A 秒杀资格判断：新增秒杀券同步 Redis 库存；Lua 原子判断库存、一人一单、扣减 Redis 库存、记录抢购用户；秒杀接口返回订单号或明确失败信息

已确认缺口：

- Kafka 异步下单未实现
- 商户缓存治理未实现
- Kafka 未接入
- Caffeine 未接入
- 订单状态流转未实现
- 限流未实现
- 智能客服未实现

## 5. 阶段计划与状态

### 阶段 1：秒杀主链路

状态：进行中；阶段 1A 已完成人工验收，阶段 1B Kafka 异步下单自动化测试已通过，等待人工验收。Kafka 使用 Docker Compose 在本地 Docker 中运行，不依赖 Windows 原生 Kafka 安装。

范围：

- 新增秒杀券时同步 Redis 库存
- Lua 原子判断库存和一人一单
- 秒杀接口返回订单号
- Kafka Producer 发送下单消息
- Kafka Consumer 异步创建订单
- MySQL 唯一索引和乐观扣减兜底

当前拆分：

- 阶段 1A：Redis + Lua 秒杀资格判断。成功时返回订单号，只预扣 Redis 库存并记录用户抢购资格，不创建 MySQL 订单，不发送 Kafka 消息。
- 阶段 1B：Kafka 环境已通过 Docker Compose 补齐，接入 Producer / Consumer，异步创建 MySQL 订单并做数据库兜底。

自动化测试计划：

- 新增秒杀券后写入 Redis 库存（红灯测试已创建：`src/test/java/com/hmdp/service/impl/VoucherServiceImplTest.java`）
- 库存不足时 Lua 返回失败
- 同一用户重复下单时 Lua 返回失败
- 库存充足且未下单时 Lua 扣减库存并记录用户
- 秒杀 Service 映射 Lua 结果，成功返回订单号，失败返回明确错误信息
- 秒杀 Controller 委托 Service
- Consumer 重复消费不会重复创建订单
- MySQL 库存扣减不会出现负数

人工验收计划：

- 新增秒杀券，检查 MySQL 与 Redis 库存。
- 单用户秒杀成功，返回订单号。
- 同一用户重复秒杀失败。
- 多用户并发秒杀后订单数量不超过库存。
- Kafka 消费后数据库订单正确生成。

### 阶段 2：订单状态流转

状态：未开始

范围：

- 订单状态字段与状态常量
- 支付接口
- 超时关单 Spring Task
- 支付与关单并发乐观锁

人工验收计划：

- 下单后订单状态为待支付。
- 支付后状态变为已支付。
- 超时未支付订单自动取消。
- 已支付订单不会被定时任务取消。
- 已取消订单不能支付。

### 阶段 3：商户缓存治理

状态：未开始

范围：

- 商户详情 Redis 缓存
- 缓存空值防穿透
- 逻辑过期防击穿
- 更新数据库后删除缓存
- 删除失败补偿重试

人工验收计划：

- 首次查询商户访问 MySQL，后续查询命中 Redis。
- 查询不存在商户后写入空值缓存。
- 热点商户逻辑过期后返回旧值并异步重建。
- 更新商户后缓存被删除。
- 缓存删除失败时触发补偿重试。

### 阶段 4：二级缓存与限流

状态：未开始

范围：

- Caffeine + Redis 二级缓存
- 本地缓存失效
- `@RateLimit` 注解
- AOP 限流切面
- 全局、IP、用户维度限流

人工验收计划：

- 首次查询走 MySQL，后续命中 Redis 和 Caffeine。
- 商户更新后本地缓存和 Redis 缓存失效。
- 高频请求触发限流。
- 不同限流维度规则生效。

### 阶段 5：智能客服

状态：未开始

范围：

- 大模型接入
- Redis 会话记忆
- Function Calling 工具白名单
- 商户查询工具
- 预约到店工具
- 参数校验和调用日志

人工验收计划：

- 多轮对话保留上下文。
- 可以通过自然语言查询商户信息。
- 可以通过工具创建预约。
- 非法参数被拒绝。
- 白名单外能力不能被调用。

## 6. 阶段测试记录

当前阶段测试记录如下。每个阶段完成后按以下格式追加：

```text
日期：2026-05-10
阶段：阶段 1A - Redis + Lua 秒杀资格判断
自动化测试命令：mvn -q -Dtest=VoucherServiceImplTest#addSeckillVoucherShouldCacheStockInRedis test
自动化测试结果：通过。验证 addSeckillVoucher 保存秒杀券后，会写入 Redis Key seckill:stock:{voucherId}，值为库存数量 12。
人工测试结论：未进行；阶段 1A 尚未完成，人工验收将在完整阶段结束后统一执行。
发现问题：首次在沙箱内运行 Maven 时因本机 Maven 仓库访问权限被拦截，提升权限后测试正常通过。
修复记录：无需修复生产代码；沿用已补充的 Redis 库存写入逻辑。
是否进入下一阶段：否。继续阶段 1A 下一个 TDD 用例：Lua 库存不足时返回失败。
```

```text
日期：2026-05-16
阶段：交接前工程最佳实践升级
自动化测试命令：mvn -q test
自动化测试结果：通过。首次回归发现 MySQL 8 场景下 `Public Key Retrieval is not allowed`，补充 JDBC 参数 `allowPublicKeyRetrieval=true` 后重新执行，全量测试通过。
人工测试结论：无需单独人工验收；本次不改变业务行为，只增强新电脑接手、环境复现和 GitHub 协作能力。
发现问题：交接文档原先容易让新电脑误以为当前机器环境验证可复用；已改成“当前开发机验证记录 + 新机器必做自检”。全量测试还暴露 MySQL 8 认证参数兼容问题。
修复记录：新增 `.env.example`、`docker-compose.dev.yml`、GitHub Actions CI、`docs/environment-setup.md`、`docs/development-workflow.md`；`application.yaml` 支持环境变量覆盖且保留本地默认值；README 和 handoff 文档补充新入口；JDBC 默认连接串补充 `allowPublicKeyRetrieval=true`。
是否进入下一阶段：否。仍等待阶段 1B 人工验收。
```

```text
日期：2026-05-10
阶段：阶段 1A - Redis + Lua 秒杀资格判断
自动化测试命令：mvn -q "-Dtest=VoucherServiceImplTest,SeckillLuaScriptTest,VoucherOrderServiceImplTest,VoucherOrderControllerTest" test
自动化测试结果：通过。覆盖新增秒杀券同步 Redis 库存、Lua 库存不足返回 1、Lua 重复下单返回 2、Lua 成功扣减库存并记录用户、Service 成功返回订单号、Service 失败返回“库存不足/不能重复下单”、Controller 委托 Service。
人工测试结论：待用户验收；阶段 1A 已进入人工验收点，验收通过后再进入 Kafka 阶段 1B。
发现问题：PowerShell 中 `-Dtest` 多类名需要加引号；并行运行两个 Maven 测试进程曾因共用 target 目录出现 `NoClassDefFoundError`，改为单进程顺序回归后通过。
修复记录：新增 `src/main/resources/lua/seckill.lua`；接入 `VoucherOrderController`、`IVoucherOrderService`、`VoucherOrderServiceImpl`；新增 `RedisIdWorker` 生成订单号；新增 Lua、Service、Controller 自动化测试。
是否进入下一阶段：否。等待阶段 1A 人工验收；Kafka 未安装，阶段 1B 暂不开始。
```

### 阶段 1A 人工验收步骤

启动服务：

1. 启动 MySQL，确认数据库为 `hmdp`，账号密码为 `root / 1234`。
2. 启动 Redis，执行 `redis-cli ping` 应返回 `PONG`。
3. 在项目根目录执行 `mvn spring-boot:run` 启动后端。
4. 由于 `/voucher-order/**` 受登录拦截器保护，先通过现有登录流程拿到 token，或临时使用已登录前端会话调用接口。

验收 1：新增秒杀券同步 Redis 库存。

- 调用已有新增秒杀券入口，提交库存为 12 的秒杀券。
- 查看 MySQL：`tb_voucher` 有一条普通券记录，`tb_seckill_voucher` 有同一 `voucher_id` 的秒杀库存记录。
- 查看 Redis：`GET seckill:stock:{voucherId}` 应返回 `12`。

验收 2：单用户秒杀成功。

- 请求：`POST http://localhost:8081/voucher-order/seckill/{voucherId}`。
- Header：携带登录 token，例如 `authorization: {token}`。
- 前置 Redis：`SET seckill:stock:{voucherId} 12`，`DEL seckill:order:{voucherId}`。
- 正确结果：响应 `success=true`，`data` 为正数订单号；`GET seckill:stock:{voucherId}` 返回 `11`；`SISMEMBER seckill:order:{voucherId} {userId}` 返回 `1`。
- 数据库预期：阶段 1A 不创建 `tb_voucher_order` 订单，Kafka 阶段 1B 才落库。

验收 3：同一用户重复秒杀失败。

- 使用同一个登录用户再次请求：`POST /voucher-order/seckill/{voucherId}`。
- 正确结果：响应 `success=false`，`errorMsg=不能重复下单`。
- Redis 预期：库存不再扣减；`seckill:order:{voucherId}` 中仍只有该用户记录。

验收 4：库存为 0 秒杀失败。

- 前置 Redis：`SET seckill:stock:{voucherId} 0`，可 `DEL seckill:order:{voucherId}` 避免重复下单分支先命中。
- 请求：`POST /voucher-order/seckill/{voucherId}`。
- 正确结果：响应 `success=false`，`errorMsg=库存不足`。
- Redis 预期：库存仍为 `0`，用户不会写入 `seckill:order:{voucherId}`。

常见失败原因：

- 未登录或 token 无效：接口被 `LoginInterceptor` 拦截，先完成登录。
- Redis Key 错误：库存 Key 必须是 `seckill:stock:{voucherId}`，用户记录 Key 必须是 `seckill:order:{voucherId}`。
- 重复下单用例误命中库存不足：确认库存 Key 大于 0。
- 库存不足用例误命中重复下单：先删除 `seckill:order:{voucherId}`。
- Kafka 没有消息或数据库没有订单：这是阶段 1A 的预期，阶段 1B 才接 Kafka 异步落库。

```text
日期：2026-05-11
阶段：阶段 1B - Kafka 异步下单
自动化测试命令：
1. mvn -q -Dtest=VoucherOrderServiceImplTest#seckillVoucherShouldReturnOrderIdWhenQualified test
2. mvn -q -Dtest=VoucherOrderConsumerTest#createVoucherOrderShouldDeductMysqlStockAndCreatePendingOrder test
3. mvn -q "-Dtest=KafkaTopicConfigTest,VoucherOrderConsumerTest" test
4. mvn -q test
自动化测试结果：通过。覆盖秒杀资格成功后发送 Kafka 下单消息、声明 Kafka Topic、Consumer 扣减 MySQL 秒杀库存并创建未支付订单、重复消息不重复扣库存/不重复建单、MySQL 库存为 0 时不创建订单。
人工测试结论：待用户验收；阶段 1B 已进入人工验收点，验收通过后再进入阶段 2。
发现问题：测试专用 application.yaml 会覆盖主配置，导致 DataSource 找不到；已改为在测试注解上关闭 Kafka Listener 自动启动，并用 MockBean 避免自动化测试依赖 KafkaAdmin 真实连接。
修复记录：新增 spring-kafka 依赖、VoucherOrderMessage、KafkaTopicConfig、VoucherOrderConsumer；秒杀 Service 成功资格判断后发送 `voucher-order-create` 消息；补充 Kafka application.yaml 配置；`hmdp.sql` 增加 `uk_user_voucher(user_id, voucher_id)` 唯一索引；新增阶段 1B SQL 辅助脚本。
是否进入下一阶段：否。等待阶段 1B 人工验收。
```

### 阶段 1B 人工验收步骤

启动服务：
1. Docker Desktop 保持运行。
2. 在 IDEA 的 Terminal 或 Windows 终端进入项目根目录 `D:\hmdp-init`，执行 `docker compose up -d`，确认 Docker Desktop 中 `hmdp-kafka` 和 `hmdp-kafka-ui` 为 Running。
3. 启动 MySQL、Redis。
4. 在 Navicat 执行一次数据库兜底索引脚本：`docs/stage-1b-voucher-order-unique-index.sql`。如果提示索引已存在，可忽略。
5. 在 IDEA 右上角运行 `HmDianPingApplication`，后端端口为 `8081`。
6. 浏览器打开 Kafka UI：`http://localhost:8088`，进入 `hmdp-local` 集群。

验收 1：确认 Kafka Topic 存在。
- Kafka UI：左侧进入 Topics。
- 正确结果：能看到 `voucher-order-create`，分区数为 1。
- 常见失败原因：Kafka 容器没启动；后端还没启动导致自动建 Topic 未触发；端口 `8088` 被占用。

验收 2：准备一个可秒杀券。
- 可沿用阶段 1A 的“新增秒杀券”接口，或用已经测试过的券 ID。
- Redis 预置：在 Another Redis Desktop Manager 中设置 `seckill:stock:{voucherId}` 为 `12`，删除 `seckill:order:{voucherId}`。
- MySQL 预置：确认 `tb_seckill_voucher` 中同一个 `voucher_id` 的 `stock` 大于 0。

验收 3：单用户秒杀成功并异步落库。
- Apifox 请求：`POST http://localhost:8081/voucher-order/seckill/{voucherId}`。
- Headers：新增一行 `authorization`，值填登录接口返回的 token。
- Body：不需要填写。
- 正确响应：`success=true`，`data` 是订单号。
- Redis 正确结果：`seckill:stock:{voucherId}` 从 `12` 变为 `11`；`seckill:order:{voucherId}` Set 中包含当前用户 ID。
- Kafka UI 正确结果：`voucher-order-create` 的 Messages 中能看到一条 JSON 消息，包含 `orderId/userId/voucherId/createTime`。
- Navicat 正确结果：`tb_voucher_order` 中新增一条订单，`id` 等于接口返回订单号，`status=1` 表示未支付；`tb_seckill_voucher.stock` 扣减 1。

验收 4：同一用户重复秒杀失败。
- Apifox 使用同一个 token，再次请求 `POST /voucher-order/seckill/{voucherId}`。
- 正确响应：`success=false`，`errorMsg=不能重复下单`。
- Redis 正确结果：库存不再扣减；Set 中仍只有该用户抢购记录。
- Navicat 正确结果：`tb_voucher_order` 不新增第二条同用户同券订单。

验收 5：MySQL 库存兜底。
- 在 Redis 中把 `seckill:stock:{voucherId}` 设置为大于 0，并删除 `seckill:order:{voucherId}`。
- 在 Navicat 中把 `tb_seckill_voucher.stock` 改为 `0`。
- Apifox 请求秒杀接口。
- 正确现象：接口可能先返回订单号，因为 Redis 资格判断通过；随后 Consumer 处理消息时数据库条件扣减失败，不会创建 `tb_voucher_order` 订单。这个用例验证的是数据库最终兜底。

常见失败原因：
- `401`：Apifox Header 没带 `authorization`，或 token 已过期。
- 返回“库存不足”：Redis 的 `seckill:stock:{voucherId}` 为 0 或不存在。
- 返回“不能重复下单”：Redis 的 `seckill:order:{voucherId}` 里已经有当前用户 ID。
- Kafka UI 没消息：后端未接上 Kafka、Kafka 容器未启动、请求没有走到成功分支。
- 接口成功但 MySQL 没订单：先等 1 到 3 秒；若仍没有，看 IDEA 控制台是否有 Kafka/Consumer 异常。
- 添加唯一索引失败：表里已有重复的 `user_id + voucher_id` 数据，需要先查重并清理测试脏数据。

```text
日期：
阶段：
自动化测试命令：
自动化测试结果：
人工测试结论：
发现问题：
修复记录：
是否进入下一阶段：
```

## 7. 交接说明

接手开发前必须确认：

1. 阅读 `docs/architecture-design.md`。
2. 阅读本文档的「阶段计划与状态」和「阶段测试记录」。
3. 查看当前代码差异。
4. 从状态为「进行中」或「未开始」的阶段继续。
5. 遵守 TDD：没有失败测试，不写生产代码。

当前下一步：

- 等待用户对阶段 1B 进行人工验收。
- 人工验收通过后，进入阶段 2：订单状态流转、超时关单、乐观锁。
