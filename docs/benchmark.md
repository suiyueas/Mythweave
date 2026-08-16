# Benchmark 基准测试

> 目标：用**可复现、可辩护**的实验数据支撑简历/README 中的性能声明。
> 原则：所有数据来自本仓库内的评测集与脚本，跑一次命令即可复现；环境信息随报告落盘，不虚报口径。

## 环境要求

| 依赖 | 说明 |
|---|---|
| Elasticsearch 8.x | 本机已启动，配置见 `application.yml` |
| QIANWEN_API_KEY | 千问 Embedding 向量化（评测集 44 文档 + 40 查询 ≈ 84 次调用，成本可忽略） |
| DEEPSEEK_API_KEY | 仅实验③真实模式需要；Mock 模式无需 |
| MySQL / Redis | `@SpringBootTest` 拉起完整应用上下文所需 |

## 实验清单

| 编号 | 实验 | 验证声明 | 产物 |
|---|---|---|---|
| ① | 混合检索 Recall@5 对比（纯向量 kNN / 纯 BM25 / kNN+BM25 混合） | 混合检索相对单一检索的召回增益 | `docs/benchmark-results/hybrid-search.md` |
| ② | 混合检索延迟 P50/P95/P99/QPS（预热 10 次 + 连续 200 次） | P95 延迟控制 | `docs/benchmark-results/hybrid-search-latency.md` |
| ③ | 多 Agent 串行 vs 并行耗时（4 Agent，默认 3 轮） | 并行调度加速比 | `docs/benchmark-results/agent-parallel.md` |

## 运行命令

```powershell
# 实验①+②（需要 ES + 千问 Key）
mvn test -Dtest=HybridSearchBenchmark -DfailIfNoTests=false

# 实验③ 真实模式（需要 DeepSeek Key，每轮 4 次 LLM 调用）
mvn test -Dtest=AgentParallelBenchmark -DfailIfNoTests=false

# 实验③ Mock 模式（零成本验证调度机制：4 个模拟 Agent 固定 800ms）
mvn test -Dtest=AgentParallelBenchmark -DfailIfNoTests=false -Dbenchmark.agent.mock=true
```

可选参数：
- `-Dbenchmark.latency.iterations=200`：实验②采样次数（默认 200）
- `-Dbenchmark.agent.rounds=5`：实验③轮数（默认真实 3 / Mock 5）

> 注意：Benchmark 类名不带 `Test` 后缀，**不会**随 `mvn test` 自动执行；且不消耗 CI。

## 评测集

`springboot/src/test/resources/benchmark/sample-data.json`

- **Corpus**：44 份玄幻小说设定文档（人物/世界观/术语/地点 4 类），写入 ES 专用 `novelId=99999`，固定 doc id 幂等覆盖，可重复运行
- **Queries**：40 条标注问题，分三类验证不同召回能力：
  - 语义类（14 条）：同义改写、无关键词重叠 → 验证向量召回
  - 关键词类（13 条）：专有名词/稀有术语（洗髓丹、封魔大阵…）→ 验证 BM25 召回
  - 混合类（13 条）：语义 + 关键词混合 → 验证融合收益
- **指标**：`Recall@5 = 命中相关文档数 / 该问题相关文档数`（相关文档数 ≤ 2 < topK，等价于 Recall@1..5）

## 结果记录

### 实验①：Recall@5（最近一次运行）

| 策略 | 语义类(14条) | 关键词类(13条) | 混合类(13条) | 整体(40条) |
|---|---|---|---|---|
| 纯向量 kNN | 待跑 | 待跑 | 待跑 | 待跑 |
| 纯 BM25 | 待跑 | 待跑 | 待跑 | 待跑 |
| 混合 kNN+BM25 | 待跑 | 待跑 | 待跑 | 待跑 |

### 实验②：延迟（最近一次运行）

| 指标 | 数值 |
|---|---|
| P50 / P95 / P99 | 待跑 |
| 平均 / QPS | 待跑 |

### 实验③：Agent 串行 vs 并行（最近一次运行）

| 模式 | 平均串行 | 平均并行 | 加速比 |
|---|---|---|---|
| Mock（2026-08-16，12 核 Win10/JDK21，2 轮） | 3239.0 ms | 809.5 ms | 4.0x |
| 真实 | 待跑（需 DeepSeek Key） | 待跑 | 待跑 |

## 口径说明（面试防御用）

1. **为什么可以复现**：评测集、脚本、环境信息全部入库，任何人克隆仓库 + 启动 ES + 配置 Key 即可重跑
2. **数据规模**：44 文档/40 问题的自建评测集——小但可辩护；这是个人开源项目的合理做法，不虚构线上用户量
3. **P95 采集方式**：JVM 内直调 `hybridSearch`（不走 HTTP），消除网络/框架噪音，反映检索本身延迟；如需端到端延迟可另行压测 HTTP 接口
4. **Mock/真实双模式**：实验③的 Mock 模式验证调度机制本身（固定 800ms/Agent，加速比理论上限 4x），真实模式反映生产管线；两者分别呈现，不混用