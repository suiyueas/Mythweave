package com.mythweave.web.service.scanner;

import com.mythweave.web.entity.NovelChapter;
import com.mythweave.web.entity.NovelSentinelAlert;
import com.mythweave.web.mapper.NovelChapterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RhythmScanner {

    private final NovelChapterMapper chapterMapper;

    /** 连续章节字数波动在此比例以下视为节奏单一（如 0.15 = 15%） */
    @Value("${app.sentinel.rhythm-monotony-threshold:0.15}")
    private double monotonyThreshold;

    /** 单章字数突增/突降超过此比例视为异常 */
    @Value("${app.sentinel.rhythm-spike-threshold:0.50}")
    private double spikeThreshold;

    /** 连续多少章节奏单一触发告警 */
    @Value("${app.sentinel.rhythm-monotony-window:3}")
    private int monotonyWindow;

    /** 过长章节字数阈值（超过平均值此倍数视为过长） */
    @Value("${app.sentinel.rhythm-long-chapter-multiplier:1.8}")
    private double longChapterMultiplier;

    /** 连续过短章节触发告警的阈值 */
    @Value("${app.sentinel.rhythm-consecutive-short:4}")
    private int consecutiveShortThreshold;

    /** 短章字数阈值 */
    @Value("${app.sentinel.rhythm-short-word-threshold:500}")
    private int shortWordThreshold;

    public List<NovelSentinelAlert> scan(Long projectId) {
        List<NovelSentinelAlert> alerts = new ArrayList<>();
        List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);

        if (chapters.size() < 3) {
            log.info("🔍 节奏巡查：章节数不足 3，跳过节奏分析");
            return alerts;
        }

        // 1. 连续字数雷同 → 节奏单一
        checkRhythmMonotony(chapters, alerts);

        // 2. 字数突增/突降检测
        checkWordCountSpikes(chapters, alerts);

        // 3. 连续章节偏长检测
        checkLongChapterStreak(chapters, alerts);

        // 4. 连续偏短检测
        checkShortChapterStreak(chapters, alerts);

        log.info("🔍 节奏巡查完成，项目 {} 共 {} 章，发现 {} 条告警",
                projectId, chapters.size(), alerts.size());
        return alerts;
    }

    /**
     * 检测连续章节字数波动是否过小（节奏单一）
     */
    private void checkRhythmMonotony(List<NovelChapter> chapters, List<NovelSentinelAlert> alerts) {
        int consecutive = 0;
        int startIdx = 0;

        for (int i = 1; i < chapters.size(); i++) {
            NovelChapter prev = chapters.get(i - 1);
            NovelChapter curr = chapters.get(i);

            if (prev.getWordCount() == null || curr.getWordCount() == null
                    || prev.getWordCount() == 0 || curr.getWordCount() == 0) {
                consecutive = 0;
                continue;
            }

            int max = Math.max(prev.getWordCount(), curr.getWordCount());
            int min = Math.min(prev.getWordCount(), curr.getWordCount());
            double ratio = (double) (max - min) / Math.max(max, 1);

            if (ratio < monotonyThreshold) {
                if (consecutive == 0) startIdx = i - 1;
                consecutive++;
                if (consecutive >= monotonyWindow) {
                    int from = chapters.get(startIdx).getSortOrder();
                    int to = curr.getSortOrder();

                    // 计算该窗口内的平均字数
                    double avgWords = 0;
                    for (int j = startIdx; j <= i; j++) {
                        avgWords += chapters.get(j).getWordCount();
                    }
                    avgWords /= (i - startIdx + 1);

                    String severity = consecutive >= monotonyWindow * 2 ? "warning" : "info";
                    alerts.add(createAlert(
                            "rhythm",
                            "连续 " + (consecutive + 1) + " 章节奏单一",
                            "第 " + from + "-" + to + " 章字数均在 " + (int) avgWords + " 字上下"
                                    + "（波动 < " + (int) (monotonyThreshold * 100) + "%），叙事节奏缺乏变化",
                            severity,
                            "建议在这些章节中调整叙事密度：穿插对话场景、动作场面或内心独白来丰富节奏变化"
                    ));
                    consecutive = 0;
                }
            } else {
                consecutive = 0;
            }
        }
    }

    /**
     * 检测字数突增/突降
     */
    private void checkWordCountSpikes(List<NovelChapter> chapters, List<NovelSentinelAlert> alerts) {
        for (int i = 1; i < chapters.size(); i++) {
            NovelChapter prev = chapters.get(i - 1);
            NovelChapter curr = chapters.get(i);

            if (prev.getWordCount() == null || curr.getWordCount() == null
                    || prev.getWordCount() == 0 || curr.getWordCount() == 0) {
                continue;
            }

            double changeRatio = (double) Math.abs(curr.getWordCount() - prev.getWordCount())
                    / Math.max(prev.getWordCount(), 1);

            if (changeRatio > spikeThreshold) {
                String direction = curr.getWordCount() > prev.getWordCount() ? "突增" : "骤降";
                String severity = changeRatio > spikeThreshold * 1.5 ? "warning" : "info";
                alerts.add(createAlert(
                        "rhythm",
                        "章节字数" + direction,
                        "第 " + curr.getSortOrder() + " 章「" + curr.getTitle() + "」字数 "
                                + curr.getWordCount() + "，较上一章（" + prev.getWordCount() + "字）"
                                + direction + "约 " + (int) (changeRatio * 100) + "%",
                        severity,
                        direction.equals("突增")
                                ? "长章节要确保有足够的情节密度支撑，避免灌水"
                                : "短章节需确认内容是否完整，是否存在未写完的段落"
                ));
            }
        }
    }

    /**
     * 检测连续偏长章节
     */
    private void checkLongChapterStreak(List<NovelChapter> chapters, List<NovelSentinelAlert> alerts) {
        // 计算平均值
        double avgWords = chapters.stream()
                .filter(c -> c.getWordCount() != null && c.getWordCount() > 0)
                .mapToInt(NovelChapter::getWordCount)
                .average()
                .orElse(0);

        if (avgWords <= 0) return;

        long threshold = (long) (avgWords * longChapterMultiplier);
        int streak = 0;
        int streakStart = 0;

        for (int i = 0; i < chapters.size(); i++) {
            NovelChapter ch = chapters.get(i);
            boolean isLong = ch.getWordCount() != null && ch.getWordCount() > threshold;

            if (isLong) {
                if (streak == 0) streakStart = i;
                streak++;
                if (streak >= 3 && (i == chapters.size() - 1 || !isLongChapter(chapters.get(i + 1), threshold))) {
                    int from = chapters.get(streakStart).getSortOrder();
                    int to = ch.getSortOrder();
                    alerts.add(createAlert(
                            "rhythm",
                            "连续 " + streak + " 章篇幅偏长",
                            "第 " + from + "-" + to + " 章均超过平均字数（" + (int) avgWords + " 字）的 "
                                    + (int) ((longChapterMultiplier - 1) * 100) + "%，"
                                    + "读者可能感到阅读疲劳",
                            "info",
                            "建议在这些章节间插入较短章节（3000-5000字）作为节奏缓冲"
                    ));
                    streak = 0;
                }
            } else {
                streak = 0;
            }
        }
    }

    private boolean isLongChapter(NovelChapter ch, long threshold) {
        return ch.getWordCount() != null && ch.getWordCount() > threshold;
    }

    /**
     * 检测连续过短章节
     */
    private void checkShortChapterStreak(List<NovelChapter> chapters, List<NovelSentinelAlert> alerts) {
        int streak = 0;
        int streakStart = 0;

        for (int i = 0; i < chapters.size(); i++) {
            NovelChapter ch = chapters.get(i);
            boolean isShort = ch.getWordCount() != null && ch.getWordCount() > 0
                    && ch.getWordCount() < shortWordThreshold;

            if (isShort) {
                if (streak == 0) streakStart = i;
                streak++;
                if (streak >= consecutiveShortThreshold) {
                    int from = chapters.get(streakStart).getSortOrder();
                    int to = ch.getSortOrder();
                    alerts.add(createAlert(
                            "rhythm",
                            "连续 " + streak + " 章篇幅偏短",
                            "第 " + from + "-" + to + " 章均不足 " + shortWordThreshold + " 字，"
                                    + "可能影响读者阅读连贯性",
                            streak >= consecutiveShortThreshold * 2 ? "warning" : "info",
                            "建议适当扩充内容，每章至少 1000-2000 字以保持叙事连贯"
                    ));
                    streak = 0;
                }
            } else {
                streak = 0;
            }
        }
    }

    private NovelSentinelAlert createAlert(String type, String title, String desc, String severity, String suggestion) {
        NovelSentinelAlert alert = new NovelSentinelAlert();
        alert.setType(type);
        alert.setTitle(title);
        alert.setDescription(desc);
        alert.setSeverity(severity);
        alert.setSuggestion(suggestion);
        return alert;
    }
}
