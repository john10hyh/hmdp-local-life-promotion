# 开发工作流

本项目采用 SPEC + TDD 的组合方式。

## 1. SPEC 定方向

涉及阶段目标、技术方案、模块边界、数据结构或关键取舍时，先更新：

```text
docs/architecture-design.md
```

如果实际实现与架构文档不一致，必须先改架构文档，再写生产代码。

## 2. TDD 保落地

每个小功能遵循：

1. 写失败测试
2. 运行测试确认红灯
3. 写最小生产代码
4. 运行测试确认绿灯
5. 必要时重构

没有失败测试，不写生产代码。

## 3. 阶段完成后更新日志

阶段完成后更新：

```text
docs/development-log.md
```

必须写清楚：

- 自动化测试命令
- 自动化测试结果
- 人工测试步骤
- 预期结果
- 常见失败原因
- 是否进入下一阶段

## 4. Git 分支建议

主分支：

```text
main
```

功能分支命名：

```text
feature/order-status
feature/shop-cache
feature/rate-limit
feature/ai-customer-service
```

修复分支命名：

```text
fix/kafka-consumer-idempotency
fix/login-token-refresh
```

## 5. 每次开发前

```bash
git status
git pull
mvn -q test
```

如果测试失败，先修复失败，不要继续开发新功能。

## 6. 每次提交前

```bash
mvn -q test
git status
```

确认测试通过后再提交：

```bash
git add .
git commit -m "feat: 描述功能"
git push
```

## 7. 提交信息建议

```text
feat: 新功能
fix: 修复问题
docs: 文档
test: 测试
refactor: 重构
chore: 工程配置
```

示例：

```text
feat: add voucher order kafka consumer
test: cover mysql stock fallback
docs: update stage 1b handoff guide
```

## 8. 当前阶段顺序

当前阶段 1B 自动化测试已完成，等待人工验收。

用户确认阶段 1B 人工验收通过后，下一阶段是：

```text
阶段 2：订单状态流转、超时关单、乐观锁
```

不要跳到缓存治理、限流或智能客服。
