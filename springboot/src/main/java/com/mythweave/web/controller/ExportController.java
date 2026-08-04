package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.entity.NovelChapter;
import com.mythweave.web.mapper.NovelChapterMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Tag(name = "导出备份")
@RestController
@RequestMapping("/api/projects/{projectId}/export")
@RequiredArgsConstructor
public class ExportController {

    private final NovelChapterMapper chapterMapper;

    private final Map<Long, List<ExportRecord>> historyStore = new ConcurrentHashMap<>();
    private final AtomicLong taskIdSeq = new AtomicLong(1);

    @Operation(summary = "获取支持的导出格式")
    @GetMapping("/formats")
    public R<List<Map<String, String>>> formats() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(Map.of("key", "txt", "label", "TXT", "icon", "\uD83D\uDCC4", "desc", "纯文本 · 通用格式"));
        list.add(Map.of("key", "md", "label", "Markdown", "icon", "\uD83D\uDCDD", "desc", "保留排版标记"));
        list.add(Map.of("key", "pdf", "label", "PDF", "icon", "\uD83D\uDCD5", "desc", "精美排版 · 可打印"));
        list.add(Map.of("key", "docx", "label", "Word", "icon", "\uD83D\uDCD8", "desc", ".docx 格式"));
        list.add(Map.of("key", "html", "label", "HTML", "icon", "\uD83C\uDF10", "desc", "网页格式"));
        return R.ok(list);
    }

    @Operation(summary = "通用导出")
    @PostMapping
    public R<Map<String, Object>> exportProject(@PathVariable Long projectId,
                                                 @RequestBody Map<String, Object> body) {
        String format = String.valueOf(body.getOrDefault("format", "txt"));
        String scope = String.valueOf(body.getOrDefault("scope", "all"));
        String layout = String.valueOf(body.getOrDefault("layout", "default"));

        log.info("导出请求: projectId={} format={} scope={} layout={}", projectId, format, scope, layout);

        List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);

        if ("custom".equals(scope) && body.containsKey("chapterIds")) {
            @SuppressWarnings("unchecked")
            List<Number> ids = (List<Number>) body.get("chapterIds");
            Set<Long> idSet = new HashSet<>();
            for (Number n : ids) idSet.add(n.longValue());
            chapters = chapters.stream().filter(c -> idSet.contains(c.getId())).toList();
        }

        StringBuilder content = new StringBuilder();
        for (NovelChapter ch : chapters) {
            content.append("# ").append(ch.getTitle()).append("\n\n");
            if (ch.getContent() != null) content.append(ch.getContent()).append("\n\n");
        }

        String fileName = "export_" + projectId + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "." + format;

        Long taskId = taskIdSeq.getAndIncrement();
        ExportRecord record = new ExportRecord();
        record.setId(taskId);
        record.setFileName(fileName);
        record.setFormat(format);
        record.setStatus("done");
        record.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        record.setFileSize(content.length());
        record.setChapterCount(chapters.size());

        historyStore.computeIfAbsent(projectId, k -> new ArrayList<>()).add(0, record);
        List<ExportRecord> list = historyStore.get(projectId);
        while (list.size() > 20) list.remove(list.size() - 1);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("fileName", fileName);
        result.put("content", content.toString());
        result.put("fileSize", content.length());
        result.put("chapterCount", chapters.size());
        result.put("message", "导出完成");
        return R.ok(result);
    }

    @Operation(summary = "获取导出历史")
    @GetMapping("/history")
    public R<List<ExportRecord>> history(@PathVariable Long projectId) {
        return R.ok(historyStore.getOrDefault(projectId, Collections.emptyList()));
    }

    @Operation(summary = "导出全书为TXT")
    @GetMapping("/txt")
    public R<String> exportTxt(@PathVariable Long projectId) {
        List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);
        StringBuilder sb = new StringBuilder();
        for (NovelChapter ch : chapters) {
            sb.append(ch.getTitle()).append("\n\n");
            sb.append(ch.getContent()).append("\n\n");
        }
        return R.ok(sb.toString());
    }

    // ─── ExportRecord 内部类 ───

    public static class ExportRecord {
        private Long id;
        private String fileName;
        private String format;
        private String status;
        private String createTime;
        private int fileSize;
        private int chapterCount;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String v) { this.fileName = v; }
        public String getFormat() { return format; }
        public void setFormat(String v) { this.format = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String v) { this.createTime = v; }
        public int getFileSize() { return fileSize; }
        public void setFileSize(int v) { this.fileSize = v; }
        public int getChapterCount() { return chapterCount; }
        public void setChapterCount(int v) { this.chapterCount = v; }
    }
}
