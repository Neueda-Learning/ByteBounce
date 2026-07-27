# ByteBounce 交易监控系统 — 任务分析文档

> 日期：2026-07-24  
> 项目：Transaction Monitoring & Alerts Dashboard  
> 规格来源：[transaction_monitoring.md](./transaction_monitoring.md)  
> 团队人数：4 人  
> 开发周期：**2.5 天**（结课项目）

---

## 目录

1. [项目概述](#1-项目概述)
2. [核心功能范围](#2-核心功能范围)
3. [数据模型设计](#3-数据模型设计)
4. [规则引擎分析](#4-规则引擎分析)
5. [告警生命周期](#5-告警生命周期)
6. [API 设计方向](#6-api-设计方向)
7. [前端功能优先级](#7-前端功能优先级)
8. [关键技术决策](#8-关键技术决策)
9. [MVP 开发路径](#9-mvp-开发路径)
10. [风险与注意事项](#10-风险与注意事项)
11. [演示准备要点](#11-演示准备要点)

---

## 1. 项目概述

ByteBounce 是一个**实时交易监控与告警管理系统**，核心目标是：

- 持续接收交易数据，并针对可配置规则进行实时评估
- 在触发规则时生成告警，并支持完整的告警生命周期管理
- 提供前端 Dashboard，供操作员查看交易、管理告警、维护规则

**项目特点：**

| 维度 | 说明 |
|------|------|
| 团队规模 | **4 人**，结课项目 |
| 开发时间 | **2.5 天**（时间极度紧张，须优先保证核心功能） |
| 用户规模 | 单操作员，无需认证/权限系统 |
| 技术方向 | REST API 后端 + Web 前端 |
| 数据存储 | 使用训练中已用的数据库技术 |
| 开发方式 | Git 分支 + PR 工作流，团队协作 |

---

## 2. 核心功能范围

### 2.1 后端（REST API）

- 交易的写入与查询
- 规则的增删改查（可配置化）
- 告警的自动生成（规则引擎触发）
- 告警生命周期管理（状态流转）

### 2.2 前端（Dashboard）

- 交易列表（过滤 + 搜索）
- 告警看板（活动告警 / 历史告警）
- 告警详情与操作（确认、调查、关闭、驳回）
- 规则配置页面

---

## 3. 数据模型设计

### 3.1 核心表结构

#### `transactions`（交易）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID / BIGINT | 主键 |
| `account_id` | VARCHAR | 发起账户 ID |
| `payee_id` | VARCHAR | 收款方 ID |
| `amount` | DECIMAL(15,2) | 交易金额 |
| `currency` | VARCHAR(3) | 货币代码（如 USD） |
| `timestamp` | TIMESTAMPTZ | 交易发生时间（UTC） |
| `status` | VARCHAR | 交易状态（COMPLETED 等） |

#### `rules`（监控规则）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID / BIGINT | 主键 |
| `name` | VARCHAR | 规则名称 |
| `type` | VARCHAR | 规则类型（AMOUNT_THRESHOLD / VELOCITY / NEW_PAYEE / DAILY_LIMIT） |
| `parameters` | JSON | 规则参数（如 threshold: 10000） |
| `enabled` | BOOLEAN | 是否启用 |
| `severity` | VARCHAR | 告警严重级别（HIGH / MEDIUM / LOW） |
| `created_at` | TIMESTAMPTZ | 创建时间 |

#### `alerts`（告警）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | UUID / BIGINT | 主键 |
| `rule_id` | FK | 触发该告警的规则 |
| `transaction_id` | FK | 触发告警的主交易 |
| `account_id` | VARCHAR | 涉及账户 |
| `status` | VARCHAR | 告警状态（见第 5 节） |
| `severity` | VARCHAR | 严重级别 |
| `message` | TEXT | 告警描述信息 |
| `created_at` | TIMESTAMPTZ | 告警生成时间 |
| `updated_at` | TIMESTAMPTZ | 最后更新时间 |
| `resolved_at` | TIMESTAMPTZ | 解决时间（CLOSED/DISMISSED 时填写） |
| `notes` | TEXT | 操作员备注 |

### 3.2 MVP 极简模型（第一版）

```
transactions: id, account_id, amount, timestamp
alerts:       id, transaction_id, status, created_at
```

> **原则：先跑通一条规则，再逐步扩展字段。**

---

## 4. 规则引擎分析

### 4.1 四种规则类型

#### 规则一：金额阈值（Amount Threshold）⭐ 优先实现

```
触发条件：单笔交易金额 > 配置阈值（如 $10,000）
实现难度：★☆☆☆☆
```

```sql
SELECT * FROM transactions
WHERE amount > :threshold
  AND timestamp > NOW() - INTERVAL '1 hour';
```

---

#### 规则二：频率规则（Velocity Rule）

```
触发条件：同一账户在 T 分钟内发生超过 N 笔交易
实现难度：★★★☆☆
```

```sql
SELECT account_id, COUNT(*) AS tx_count
FROM transactions
WHERE timestamp > NOW() - INTERVAL ':minutes minutes'
GROUP BY account_id
HAVING COUNT(*) > :max_count;
```

**注意：** 必须在 `(account_id, timestamp)` 上建立**复合索引**。

---

#### 规则三：新收款方（New Payee Rule）

```
触发条件：某账户首次向该 payee_id 发起交易
实现难度：★★☆☆☆
```

```sql
SELECT COUNT(*) AS prev_count
FROM transactions
WHERE account_id = :account_id
  AND payee_id   = :payee_id
  AND timestamp  < :current_tx_timestamp;
-- 若 prev_count = 0，则触发告警
```

---

#### 规则四：每日累计限额（Daily Limit Rule）

```
触发条件：某账户当日累计交易金额 > 配置限额（如 $50,000）
实现难度：★★★☆☆
```

```sql
SELECT account_id, SUM(amount) AS daily_total
FROM transactions
WHERE DATE(timestamp) = CURRENT_DATE
GROUP BY account_id
HAVING SUM(amount) > :daily_limit;
```

---

### 4.2 规则引擎设计模式（推荐）

采用 **Strategy 模式**，每种规则类型实现统一接口：

```
RuleEvaluator (interface)
    └── evaluate(transaction) → AlertResult?

AmountThresholdRule implements RuleEvaluator
VelocityRule        implements RuleEvaluator
NewPayeeRule        implements RuleEvaluator
DailyLimitRule      implements RuleEvaluator
```

优点：新增规则类型无需修改核心代码，符合**开闭原则**。

---

## 5. 告警生命周期

### 5.1 状态机

```
OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED
             ↓               ↓
         DISMISSED       DISMISSED
```

### 5.2 状态定义

| 状态 | 含义 | 可转换至 |
|------|------|----------|
| `OPEN` | 告警已生成，待人工审核 | ACKNOWLEDGED, DISMISSED |
| `ACKNOWLEDGED` | 操作员已查看，尚未调查 | INVESTIGATING, DISMISSED |
| `INVESTIGATING` | 正在主动调查中 | CLOSED, DISMISSED |
| `CLOSED` | 调查完毕，问题已处理或确认合法 | — |
| `DISMISSED` | 判断为误报，无需处理 | — |

### 5.3 状态转换 API

```
PATCH /alerts/{id}/acknowledge   → OPEN → ACKNOWLEDGED
PATCH /alerts/{id}/investigate   → ACKNOWLEDGED → INVESTIGATING
PATCH /alerts/{id}/close         → INVESTIGATING → CLOSED
PATCH /alerts/{id}/dismiss       → ACKNOWLEDGED / INVESTIGATING → DISMISSED
```

---

## 6. API 设计方向

### 6.1 交易端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/transactions` | 提交新交易（触发规则评估） |
| `GET` | `/transactions` | 查询交易列表（支持过滤/分页） |
| `GET` | `/transactions/{id}` | 查询单笔交易详情 |

### 6.2 告警端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/alerts` | 查询告警列表（可按状态过滤） |
| `GET` | `/alerts/{id}` | 查询告警详情 |
| `PATCH` | `/alerts/{id}/acknowledge` | 确认告警 |
| `PATCH` | `/alerts/{id}/investigate` | 开始调查 |
| `PATCH` | `/alerts/{id}/close` | 关闭告警 |
| `PATCH` | `/alerts/{id}/dismiss` | 驳回告警 |

### 6.3 规则端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/rules` | 查询所有规则 |
| `POST` | `/rules` | 创建新规则 |
| `PUT` | `/rules/{id}` | 更新规则参数 |
| `PATCH` | `/rules/{id}/enable` | 启用规则 |
| `PATCH` | `/rules/{id}/disable` | 禁用规则 |
| `DELETE` | `/rules/{id}` | 删除规则 |

---

## 7. 前端功能优先级

| 优先级 | 功能 | 说明 |
|--------|------|------|
| P0 | 交易列表 | 支持金额/账户/时间范围过滤 + 关键词搜索 |
| P0 | 活动告警看板 | 展示 OPEN / ACKNOWLEDGED / INVESTIGATING 状态的告警 |
| P1 | 告警详情页 | 显示告警信息 + 触发交易 + 操作按钮 |
| P1 | 确认告警操作 | 一键 Acknowledge |
| P1 | 关闭/驳回告警 | 附带备注输入框 |
| P2 | 历史告警 | 展示 CLOSED / DISMISSED 告警 |
| P2 | 规则管理页 | 查看、启用/禁用、编辑规则参数 |

---

## 8. 关键技术决策

### 8.1 规则评估时机

| 方案 | 优点 | 缺点 | 建议 |
|------|------|------|------|
| **同步评估**（交易写入时立即执行） | 实现简单，延迟低 | 高并发时阻塞写入 | MVP 阶段使用 |
| **异步评估**（消息队列解耦） | 写入快，可横向扩展 | 需引入 MQ（如 Kafka/RabbitMQ） | 进阶阶段实现 |

### 8.2 时间处理规范

- 所有时间字段内部统一存储为 **UTC**
- API 响应使用 **ISO 8601** 格式（如 `2026-07-24T08:00:00Z`）
- 前端展示时按用户时区转换

### 8.3 性能关键点

- `transactions` 表必须在 `(account_id, timestamp)` 上建**复合索引**
- `(payee_id, account_id)` 建索引，加速 New Payee 查询
- 大量历史数据时考虑**分区表**（按月分区）

### 8.4 告警质量

- 实现**告警去重**：同一账户同一规则在短时间内不重复告警
- 实现**告警分级**（HIGH / MEDIUM / LOW）以减少告警疲劳

---

## 9. MVP 开发路径（4人 × 2.5天）

> **核心原则：时间有限，必须砍需求。先跑通演示主线，再按时间余量补充。**

### 9.1 人员分工建议

| 角色 | 负责方向 |
|------|----------|
| 后端 A | 数据库模型、Transaction API、规则引擎 |
| 后端 B | Alert API、状态流转、数据模拟器 |
| 前端 A | 交易列表页、告警看板页 |
| 前端 B | 告警详情页、规则管理页、整体样式联调 |

> 两位后端先合力搭好项目骨架（约半天），再各自分工；前端等后端第一个接口就绪即可并行开发（可用 mock 数据先行）。

### 9.2 时间计划（2.5天）

```
第 1 天上午（全员）
  ✦ 确定技术栈、创建 Git 仓库、搭建项目骨架
  ✦ 确定数据模型（Transaction / Alert 两张表，字段极简）
  ✦ 配置数据库连接，跑通第一个接口

第 1 天下午
  [后端 A] Transaction 表 + POST /transactions
           + 硬编码金额阈值规则（同步触发告警）
  [后端 B] Alert 表 + GET /alerts + 状态流转 API
           （acknowledge / close / dismiss）
  [前端]   用 mock 数据搭好页面框架，交易列表 + 告警看板

第 2 天上午
  [后端 A] Velocity Rule + New Payee Rule
  [后端 B] Rule 表 + 规则 CRUD API + 交易数据模拟器
  [前端 A] 对接真实 API，交易列表过滤/搜索
  [前端 B] 告警详情页 + 状态操作按钮联调

第 2 天下午
  [后端 A] Daily Limit Rule（如有时间）+ Bug 修复
  [后端 B] 联调支持，整理简单 API 说明
  [前端]   规则管理页 + 整体 UI 打磨 + Bug 修复
  [全员]   端到端联调，确保演示路径无报错

第 3 天上午（半天，冲刺收尾）
  ✦ 演示彩排：走完完整 Demo 路径至少 2 遍
  ✦ 准备演示 PPT（架构图、数据模型图）
  ✦ 修复最后的 Bug，冻结代码
  ✦ 确认每人的演讲内容分工
```

### 9.3 必须完成（Must Have）

- [ ] 交易写入 + 金额阈值规则触发告警
- [ ] 告警状态流转（OPEN → ACKNOWLEDGED → CLOSED / DISMISSED）
- [ ] 交易列表页（含基本过滤）
- [ ] 告警看板页（活动告警）
- [ ] 告警详情 + 操作按钮

### 9.4 时间允许再做（Nice to Have）

- [ ] Velocity Rule / New Payee Rule / Daily Limit Rule
- [ ] 规则可配置化（Rule 表 + 管理页）
- [ ] 告警历史页
- [ ] 交易数据模拟器
- [ ] Swagger/OpenAPI 文档

---

## 10. 风险与注意事项

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| **时间不够用** ⚠️ | 核心功能无法完成 | 严格执行 Must Have 清单，Nice to Have 坚决延后 |
| 数据模型设计过于复杂 | 难以迭代，浪费时间 | 两张表先跑通，字段按需扩展 |
| 前后端接口联调阻塞 | 前端无法开发 | 提前约定接口字段格式，前端用 mock 数据并行开发 |
| 4人 Git 冲突频繁 | 合并耗时 | 明确分支职责，后端/前端各一条主分支，频繁提 PR |
| Velocity Rule 实现复杂 | 超时未完成 | 优先级排在金额阈值之后，时间不够可跳过 |
| 演示时出现 Bug | 演示效果差 | 第 2 天下午冻结代码，第 3 天上午彩排至少 2 遍 |

---

## 11. 演示准备要点

### 演示时长：约 20 分钟（4人组）

### 4人演讲分工建议

| 演讲段落 | 时长 | 建议由谁讲 |
|----------|------|------------|
| 团队介绍 + 项目背景 | 2分钟 | 组长 |
| 技术架构 + 数据模型 | 3分钟 | 后端 A |
| Live Demo 操作 | 8分钟 | 后端 B 或前端成员 |
| 挑战与反思 + 下一步 | 4分钟 | 前端成员 |
| 问答环节 | 3分钟 | 全员 |

### 建议演示流程

1. **介绍团队**（2分钟）：4位成员各自简介
2. **介绍项目背景**：学了什么技术，被要求做什么，只有 2.5 天时间
3. **技术架构图**：后端框架、数据库、前端技术栈，一张简图
4. **数据模型讲解**：展示 Transaction / Alert / Rule 三张表，说明设计取舍
5. **Live Demo**（核心，约 8 分钟）：
   - 用模拟器批量写入交易数据
   - 提交一笔超额交易 → 触发金额阈值告警，Dashboard 出现告警
   - 演示告警确认 → 调查 → 关闭流程
   - 展示规则管理页（如已完成）
6. **挑战与反思**：2.5 天内的取舍决策，遇到的技术问题
7. **如果时间充裕**：下一步会做什么

### 注意事项

- **每位成员都必须发言**
- 保持摄像头开启
- 第 3 天上午完成至少 2 次完整彩排
- 演示数据提前准备好，避免现场手动输入出错
- 准备好向其他组提问（每组必须提至少一个问题）

---

*文档版本：v1.1 — 2026-07-24 | 更新：补充 4 人团队 / 2.5 天结课项目约束*
