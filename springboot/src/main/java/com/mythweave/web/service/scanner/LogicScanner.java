package com.mythweave.web.service.scanner;

import com.mythweave.web.entity.NovelChapter;
import com.mythweave.web.entity.NovelCharacter;
import com.mythweave.web.entity.NovelSentinelAlert;
import com.mythweave.web.mapper.NovelChapterMapper;
import com.mythweave.web.mapper.NovelCharacterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogicScanner {

    private final NovelChapterMapper chapterMapper;
    private final NovelCharacterMapper characterMapper;

    /** 章节字数过低阈值（小于此值视为内容异常） */
    @Value("${app.sentinel.logic-min-words:50}")
    private int minWordsThreshold;

    /** 连续短章告警阈值 */
    @Value("${app.sentinel.logic-consecutive-short:3}")
    private int consecutiveShortThreshold;

    public List<NovelSentinelAlert> scan(Long projectId) {
        List<NovelSentinelAlert> alerts = new ArrayList<>();

        List<NovelChapter> chapters = chapterMapper.selectByProjectId(projectId);

        // 1. 章节元数据异常检测
        checkChapterAnomalies(chapters, alerts);

        // 2. 章节排序断档检测
        checkChapterSequence(chapters, alerts);

        // 3. 角色关系格式检测
        checkCharacterRelations(projectId, alerts);

        log.info("🔍 逻辑巡查完成，项目 {} 共 {} 章，发现 {} 条告警",
                projectId, chapters.size(), alerts.size());
        return alerts;
    }

    private void checkChapterAnomalies(List<NovelChapter> chapters, List<NovelSentinelAlert> alerts) {
        for (NovelChapter ch : chapters) {
            // 字数异常
            if (ch.getWordCount() != null && ch.getWordCount() < minWordsThreshold && ch.getWordCount() > 0) {
                alerts.add(createAlert(
                        "logic",
                        "章节字数异常偏低",
                        "第 " + ch.getSortOrder() + " 章「" + ch.getTitle() + "」仅 " + ch.getWordCount() + " 字，"
                                + "低于最小阈值 " + minWordsThreshold + " 字",
                        "info",
                        "检查该章节内容是否完整，是否需要补充描写或情节"
                ));
            }

            // 字数缺失
            if (ch.getWordCount() == null || ch.getWordCount() == 0) {
                if (ch.getContent() != null && !ch.getContent().isEmpty()) {
                    alerts.add(createAlert(
                            "logic",
                            "章节字数统计异常",
                            "第 " + ch.getSortOrder() + " 章「" + ch.getTitle() + "」有内容但字数统计为 0",
                            "info",
                            "建议重新保存该章节以触发字数重算"
                    ));
                }
            }
        }

        // 连续短章检测
        int shortCount = 0;
        for (NovelChapter ch : chapters) {
            if (ch.getWordCount() != null && ch.getWordCount() < minWordsThreshold) {
                shortCount++;
                if (shortCount == consecutiveShortThreshold) {
                    int startIdx = chapters.indexOf(ch) - consecutiveShortThreshold + 1;
                    int startChapter = chapters.get(Math.max(startIdx, 0)).getSortOrder();
                    alerts.add(createAlert(
                            "logic",
                            "连续多章字数偏低",
                            "第 " + startChapter + " 章起连续 " + consecutiveShortThreshold + " 章字数低于 "
                                    + minWordsThreshold + " 字，可能存在内容断档",
                            "warning",
                            "检查最近创作的章节内容是否完整，是否存在未完成的片段"
                    ));
                }
            } else {
                shortCount = 0;
            }
        }
    }

    private void checkChapterSequence(List<NovelChapter> chapters, List<NovelSentinelAlert> alerts) {
        if (chapters.size() < 2) return;

        List<Integer> sortOrders = chapters.stream()
                .map(NovelChapter::getSortOrder)
                .sorted()
                .collect(Collectors.toList());

        // 检查排序是否连续
        for (int i = 0; i < sortOrders.size() - 1; i++) {
            int gap = sortOrders.get(i + 1) - sortOrders.get(i);
            if (gap > 1) {
                alerts.add(createAlert(
                        "logic",
                        "章节排序存在断档",
                        "第 " + sortOrders.get(i) + " 章与第 " + sortOrders.get(i + 1) + " 章之间缺少 "
                                + (gap - 1) + " 个章节序号，可能存在章节删除或排序异常",
                        "info",
                        "检查章节管理中的排序是否正常，可执行「重新排序」修复"
                ));
            }
        }
    }

    private void checkCharacterRelations(Long projectId, List<NovelSentinelAlert> alerts) {
        List<NovelCharacter> characters = characterMapper.selectByProjectId(projectId);

        for (NovelCharacter ch : characters) {
            // 关系字段为空但角色是主要角色
            if ((ch.getRelation() == null || ch.getRelation().isBlank())
                    && isMajorCharacter(ch)) {
                alerts.add(createAlert(
                        "logic",
                        "主要角色「" + ch.getName() + "」缺少关系设定",
                        ch.getName() + "为" + getRoleLabel(ch.getRole()) + "，但「关系网络」字段为空，"
                                + "会影响 AI 上下文理解角色互动",
                        "info",
                        "建议填写该角色与其他角色的关系，格式：角色名:关系（如：张三:师徒）"
                ));
            }

            // 弧光设定不完整
            if (ch.getArcStart() != null && !ch.getArcStart().isEmpty()
                    && (ch.getArcEnd() == null || ch.getArcEnd().isEmpty())) {
                alerts.add(createAlert(
                        "logic",
                        "角色「" + ch.getName() + "」弧光设定不完整",
                        ch.getName() + "设定了弧光起点「" + ch.getArcStart() + "」但未设定弧光目标",
                        "info",
                        "建议补充弧光终点（成长目标），以便追踪角色的成长轨迹"
                ));
            }
        }
    }

    private boolean isMajorCharacter(NovelCharacter ch) {
        String role = ch.getRole();
        return "主角".equals(role) || "protagonist".equals(role)
                || "重要配角".equals(role) || "main".equals(role);
    }

    private String getRoleLabel(String role) {
        if (role == null) return "角色";
        switch (role) {
            case "protagonist": return "主角";
            case "main": return "重要配角";
            case "supporting": return "配角";
            case "antagonist": return "反派";
            default: return role;
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
