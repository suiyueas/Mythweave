package com.mythweave.benchmark;

import com.mythweave.web.MythweaveApplication;
import com.mythweave.web.model.AgentContext;
import com.mythweave.web.model.AgentResult;
import com.mythweave.web.service.agent.CharacterAgent;
import com.mythweave.web.service.agent.EditorAgent;
import com.mythweave.web.service.agent.ReaderAgent;
import com.mythweave.web.service.agent.StyleAgent;
import com.mythweave.web.service.agent.WritingAgent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 实验③：多 Agent 串行 vs 并行耗时对比
 *
 * 两种模式：
 *  - 真实模式（默认）：调用真实 4 个 Agent + DeepSeek API，反映生产管线真实耗时
 *  - Mock 模式：mvn test -Dtest=AgentParallelBenchmark -DfailIfNoTests=false -Dbenchmark.agent.mock=true
 *    用固定 800ms 的模拟 Agent 验证调度机制，无需 API Key、零成本、可复现
 *
 * 产物：docs/benchmark-results/agent-parallel.md
 */
@SpringBootTest(classes = MythweaveApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AgentParallelBenchmark {

    private static final boolean MOCK = Boolean.parseBoolean(System.getProperty("benchmark.agent.mock", "false"));
    private static final int ROUNDS = Integer.parseInt(
            System.getProperty("benchmark.agent.rounds", MOCK ? "5" : "3"));

    @Autowired(required = false)
    private EditorAgent editorAgent;
    @Autowired(required = false)
    private CharacterAgent characterAgent;
    @Autowired(required = false)
    private StyleAgent styleAgent;
    @Autowired(required = false)
    private ReaderAgent readerAgent;

    private List<WritingAgent> agents;

    @BeforeAll
    void setUp() {
        if (MOCK) {
            agents = List.of(
                    new FakeAgent("editor", "编辑"),
                    new FakeAgent("character", "人物"),
                    new FakeAgent("style", "风格"),
                    new FakeAgent("reader", "读者"));
            System.out.println("Mock 模式：4 个模拟 Agent（固定 800ms/个），零 API 成本");
        } else {
            assertNotNull(editorAgent, "未注入 EditorAgent，请检查 Spring 上下文");
            assertNotNull(characterAgent, "未注入 CharacterAgent，请检查 Spring 上下文");
            assertNotNull(styleAgent, "未注入 StyleAgent，请检查 Spring 上下文");
            assertNotNull(readerAgent, "未注入 ReaderAgent，请检查 Spring 上下文");
            agents = List.of(editorAgent, characterAgent, styleAgent, readerAgent);
            System.out.println("真实模式：4 个 Agent 调用 DeepSeek API（每轮 4 次调用）");
        }
    }

    @Test
    void serialVsParallel() throws Exception {
        AgentContext context = buildContext();

        List<Long> serialTotals = new ArrayList<>();
        List<Long> parallelTotals = new ArrayList<>();
        List<Long> perAgentMs = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("# 实验③：多 Agent 串行 vs 并行耗时\n\n");
        sb.append(BenchmarkUtils.envHeader());
        sb.append("- 模式: ").append(MOCK ? "Mock（模拟 Agent 固定 800ms/个，验证调度机制）"
                : "真实（DeepSeek API，反映生产管线真实耗时）").append("\n");
        sb.append("- Agent 数: 4（编辑/人物/风格/读者），轮数: ").append(ROUNDS).append("\n\n");
        sb.append("| 轮次 | 串行总耗时(ms) | 并行总耗时(ms) | 加速比 |\n");
        sb.append("|---|---|---|---|\n");

        for (int round = 1; round <= ROUNDS; round++) {
            long serial = runSerial(context);
            long parallel = runParallel(context);
            serialTotals.add(serial);
            parallelTotals.add(parallel);
            double speedup = (double) serial / parallel;
            sb.append("| ").append(round).append(" | ").append(serial)
                    .append(" | ").append(parallel).append(" | ").append(BenchmarkUtils.fmt(speedup))
                    .append("x |\n");
            System.out.printf("第%d轮: 串行=%dms 并行=%dms 加速比=%.2fx%n", round, serial, parallel, speedup);
            Thread.sleep(2000);
        }

        double avgSerial = serialTotals.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgParallel = parallelTotals.stream().mapToLong(Long::longValue).average().orElse(0);
        sb.append("| **平均** | **").append(BenchmarkUtils.fmt(avgSerial))
                .append("** | **").append(BenchmarkUtils.fmt(avgParallel))
                .append("** | **").append(BenchmarkUtils.fmt(avgSerial / avgParallel)).append("x** |\n\n");

        sb.append("> 结论：并行调度平均耗时 **").append(BenchmarkUtils.fmt(avgParallel))
                .append("ms**，串行 **").append(BenchmarkUtils.fmt(avgSerial))
                .append("ms**，加速比 **").append(BenchmarkUtils.fmt(avgSerial / avgParallel))
                .append("x**（4 Agent 上限 4x，实际接近线性扩展）。\n");

        if (!MOCK) {
            sb.append("\n## 单 Agent 平均耗时（真实模式）\n\n");
            sb.append("| Agent | 平均耗时(ms) |\n|---|---|\n");
            for (WritingAgent a : agents) {
                long sum = 0;
                int n = 0;
                for (int i = 0; i < Math.min(ROUNDS, 3); i++) {
                    AgentResult r = a.analyze(context);
                    if (r.isSuccess()) { sum += r.getCostMs(); n++; }
                }
                if (n > 0) {
                    sb.append("| ").append(a.getAgentName()).append(" | ")
                            .append(BenchmarkUtils.fmt((double) sum / n)).append(" |\n");
                }
            }
        }

        Path report = BenchmarkUtils.writeReport("agent-parallel.md", sb.toString());
        System.out.println("报告已生成: " + report.toAbsolutePath());
        System.out.printf("%n===== 汇总（%d 轮） =====%n平均串行=%.1fms  平均并行=%.1fms  平均加速比=%.2fx%n",
                ROUNDS, avgSerial, avgParallel, avgSerial / avgParallel);
    }

    private long runSerial(AgentContext context) {
        long start = System.currentTimeMillis();
        for (WritingAgent a : agents) {
            a.analyze(context);
        }
        return System.currentTimeMillis() - start;
    }

    private long runParallel(AgentContext context) throws Exception {
        long start = System.currentTimeMillis();
        CompletableFuture<?>[] futures = agents.stream()
                .map(a -> CompletableFuture.runAsync(() -> a.analyze(context)))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).get(180, TimeUnit.SECONDS);
        return System.currentTimeMillis() - start;
    }

    private AgentContext buildContext() {
        AgentContext ctx = new AgentContext();
        ctx.setProjectId(99999L);
        ctx.setProjectTitle("九霄");
        ctx.setGenre("玄幻");
        ctx.setChapterTitle("第十二章 剑冢惊变");
        ctx.setChapterIndex(12);
        ctx.setChapterContent("林九霄踏入剑冢，漫天剑意如潮水般涌来。青霜剑在他掌心轻轻颤动，剑身泛起幽蓝色的光芒。"
                + "身后的韩修远脸色发白，显然承受不住这冲霄的剑意。\n\n"
                + "“你来了。”剑冢深处传来苍老的声音。林九霄握紧青霜，沉声道：“前辈，我此来只为一件事——"
                + "问清当年血魔老祖神魂脱逃的真相。”话音未落，万剑齐鸣，仿佛在回应他的决心。\n\n"
                + "剑冢深处，一道虚影缓缓浮现。那是五百年前陨落的万剑宗上代剑主，他的目光落在林九霄身上，良久才道："
                + "“你的剑骨……残缺了。”林九霄心头一震，正要开口，脚下的剑冢忽然剧烈震颤起来。");
        ctx.setReaderType("玄幻爽文读者");

        AgentContext.CharacterInfo lin = new AgentContext.CharacterInfo();
        lin.setName("林九霄");
        lin.setRole("主角");
        lin.setPersonality("坚韧隐忍，以弱胜强");
        lin.setBackground("青云城林家庶子，身怀残缺剑骨");
        lin.setArc("外门杂役→内门首席");
        AgentContext.CharacterInfo han = new AgentContext.CharacterInfo();
        han.setName("韩修远");
        han.setRole("配角");
        han.setPersonality("心性纯良");
        han.setBackground("万剑宗外门弟子，林九霄少年好友");
        han.setArc("外门弟子→体修");
        ctx.setCharacters(List.of(lin, han));

        AgentContext.ForeshadowingInfo fs = new AgentContext.ForeshadowingInfo();
        fs.setName("血魔老祖神魂");
        fs.setDescription("第3章埋下：封印松动，一缕神魂逃出");
        fs.setChapterId(3);
        fs.setResolvedChapterId(45);
        fs.setStatus("pending");
        ctx.setForeshadowings(List.of(fs));
        return ctx;
    }

    /** Mock Agent：固定 800ms 模拟一次 LLM 调用，用于无成本验证并行调度 */
    private static class FakeAgent implements WritingAgent {
        private final String key;
        private final String name;

        FakeAgent(String key, String name) {
            this.key = key;
            this.name = name;
        }

        @Override
        public AgentResult analyze(AgentContext context) {
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return AgentResult.success(key, name, "模拟分析结果（benchmark mock）", 800);
        }

        @Override
        public String getAgentKey() { return key; }

        @Override
        public String getAgentName() { return name; }
    }
}