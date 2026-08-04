package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.config.AiProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统状态：AI 服务 / 数据库 / 存储的真实探测接口
 */
@Slf4j
@Tag(name = "系统状态")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final AiProperties aiProperties;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    private final OkHttpClient probeClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(5))
            .build();

    @Operation(summary = "获取系统真实状态（AI服务/数据库/存储）")
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ai", probeAi());
        result.put("es", Map.of("status", "not_configured", "message", "未接入 Elasticsearch，向量检索暂不可用"));
        result.put("db", probeDb());
        result.put("storage", probeStorage());
        return R.ok(result);
    }

    /** 探测 AI 服务：调用 DeepSeek /models 接口（不消耗 token），测量真实延迟 */
    private Map<String, Object> probeAi() {
        AiProperties.DeepSeek cfg = aiProperties.getDeepseek();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            return Map.of("status", "warning", "latency", 0, "message", "未配置 DeepSeek API Key");
        }
        long start = System.currentTimeMillis();
        try {
            Request request = new Request.Builder()
                    .url(cfg.getBaseUrl() + "/models")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .get()
                    .build();
            try (Response response = probeClient.newCall(request).execute()) {
                int latency = (int) (System.currentTimeMillis() - start);
                if (response.isSuccessful()) {
                    return Map.of("status", "healthy", "latency", latency, "message", "模型: " + cfg.getModel());
                }
                return Map.of("status", "error", "latency", latency, "message", "AI 服务响应异常（HTTP " + response.code() + "）");
            }
        } catch (IOException e) {
            int latency = (int) (System.currentTimeMillis() - start);
            log.warn("AI 服务探测失败: {}", e.getMessage());
            return Map.of("status", "error", "latency", latency, "message", "AI 服务不可达：" + e.getMessage());
        }
    }

    /** 探测数据库：SELECT 1 计时 + 连接池占用（反射读取，避免强依赖 HikariCP 类型） */
    private Map<String, Object> probeDb() {
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            int latency = (int) (System.currentTimeMillis() - start);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("status", "healthy");
            info.put("latency", latency);
            try {
                java.lang.reflect.Method getActive = dataSource.getClass().getMethod("getActiveConnections");
                java.lang.reflect.Method getMax = dataSource.getClass().getMethod("getMaximumPoolSize");
                int active = (Integer) getActive.invoke(dataSource);
                int total = (Integer) getMax.invoke(dataSource);
                int poolUsage = total > 0 ? (int) Math.round(active * 100.0 / total) : 0;
                info.put("poolUsage", poolUsage);
                info.put("message", "连接池 " + active + "/" + total);
            } catch (Exception ignore) {
                info.put("poolUsage", 0);
                info.put("message", "数据库连接正常");
            }
            return info;
        } catch (Exception e) {
            log.warn("数据库探测失败: {}", e.getMessage());
            return Map.of("status", "error", "latency", (int) (System.currentTimeMillis() - start),
                    "poolUsage", 0, "message", "数据库不可达：" + e.getMessage());
        }
    }

    /** 探测存储：服务器磁盘占用 */
    private Map<String, Object> probeStorage() {
        try {
            File dir = new File(".").getAbsoluteFile();
            java.nio.file.FileStore store = Files.getFileStore(dir.toPath());
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long used = total - usable;
            int usedPercent = total > 0 ? (int) Math.round(used * 100.0 / total) : 0;
            return Map.of(
                    "status", usedPercent >= 90 ? "warning" : "healthy",
                    "usedPercent", usedPercent,
                    "used", formatSize(used),
                    "free", formatSize(usable),
                    "message", "服务器磁盘（" + store.name() + "）"
            );
        } catch (Exception e) {
            log.warn("存储探测失败: {}", e.getMessage());
            return Map.of("status", "warning", "usedPercent", 0, "used", "-", "free", "-",
                    "message", "存储探测失败：" + e.getMessage());
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        return String.format("%.1f GB", mb / 1024.0);
    }
}
