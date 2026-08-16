package com.mythweave.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 基准测试公共工具：
 * 加载评测集 JSON、百分位统计、生成 Markdown 报告
 */
public final class BenchmarkUtils {

    private BenchmarkUtils() {}

    public static final long BENCH_NOVEL_ID = 99999L;

    /**
     * 报告统一输出到仓库根目录 docs/benchmark-results/：
     * 从工作目录向上定位到同时包含 springboot/ 与 vue/ 的仓库根，规避 mvn 与 IDE 工作目录差异
     */
    public static Path reportDir() {
        Path current = Paths.get("").toAbsolutePath().normalize();
        while (current != null) {
            if (java.nio.file.Files.isDirectory(current.resolve("springboot"))
                    && java.nio.file.Files.isDirectory(current.resolve("vue"))) {
                return current.resolve("docs").resolve("benchmark-results");
            }
            current = current.getParent();
        }
        return Paths.get("docs", "benchmark-results");
    }

    public static BenchmarkData loadBenchmarkData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = BenchmarkUtils.class.getClassLoader()
                .getResourceAsStream("benchmark/sample-data.json")) {
            if (in == null) {
                throw new IOException("找不到评测集: benchmark/sample-data.json (springboot/src/test/resources/benchmark/)");
            }
            return mapper.readValue(in, BenchmarkData.class);
        }
    }

    public static double percentile(long[] samples, double p) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        int idx = (int) Math.ceil(p / 100.0 * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(idx, sorted.length - 1))];
    }

    public static long percentileMs(long[] samples, double p) {
        return Math.round(percentile(samples, p));
    }

    public static String envHeader() {
        return "## 运行环境\n"
                + "- 运行时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n"
                + "- OS: " + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + "\n"
                + "- JDK: " + System.getProperty("java.version") + "\n"
                + "- CPU 核数(可用): " + Runtime.getRuntime().availableProcessors() + "\n";
    }

    public static Path writeReport(String filename, String content) throws IOException {
        Path dir = reportDir();
        Files.createDirectories(dir);
        Path file = dir.resolve(filename);
        Files.writeString(file, content);
        return file;
    }

    public static String fmt(double v) {
        return String.format("%.1f", v);
    }
}