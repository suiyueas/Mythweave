package com.novelcraft.web.service.scanner;

import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelCharacter;
import com.novelcraft.web.entity.NovelSentinelAlert;
import com.novelcraft.web.mapper.NovelChapterMapper;
import com.novelcraft.web.mapper.NovelCharacterMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CharacterScanner {

    private final NovelCharacterMapper characterMapper;
    private final NovelChapterMapper chapterMapper;

    /** 主角超过此章节数未出场则告警 */
    @Value("${app.sentinel.character-protagonist-threshold:5}")
    private int protagonistThreshold;

    /** 配角超过此章节数未出场则告警 */
    @Value("${app.sentinel.character-supporting-threshold:10}")
    private int supportingThreshold;

    /** 弧光进度在此值以下视为停滞 */
    @Value("${app.sentinel.character-arc-stall-threshold:20}")
    private int arcStallThreshold;

    public List<NovelSentinelAlert> scan(Long projectId) {
        List<NovelSentinelAlert> alerts = new ArrayList<>();
        List<NovelCharacter> characters = characterMapper.selectByProjectId(projectId);
        Integer currentChapter = chapterMapper.selectMaxSortOrder(projectId);
        if (currentChapter == null) currentChapter = 0;

        for (NovelCharacter ch : characters) {
            // 1. 长期未出场检测
            checkLongAbsence(ch, currentChapter, alerts);

            // 2. 弧光进度停滞检测
            checkArcStagnation(ch, alerts);
        }

        log.info("🔍 人物巡查完成，项目 {} 共 {} 个角色，发现 {} 条告警",
                projectId, characters.size(), alerts.size());
        return alerts;
    }

    private void checkLongAbsence(NovelCharacter ch, int currentChapter, List<NovelSentinelAlert> alerts) {
        List<NovelChapter> publishedChapters = chapterMapper.selectPublishedChapters(ch.getProjectId());
        if (publishedChapters.isEmpty()) {
            return;
        }

        String characterName = ch.getName();
        int firstThreeChaptersCount = 0;
        int totalOccurrences = 0;
        int firstAppearanceChapter = -1;
        int lastSeenChapter = 0;

        for (int i = 0; i < publishedChapters.size(); i++) {
            NovelChapter chapter = publishedChapters.get(i);
            String content = chapter.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }

            int countInChapter = countOccurrences(content, characterName);
            if (countInChapter > 0) {
                totalOccurrences += countInChapter;
                lastSeenChapter = chapter.getSortOrder();
                if (firstAppearanceChapter == -1) {
                    firstAppearanceChapter = chapter.getSortOrder();
                }
                if (i < 3) {
                    firstThreeChaptersCount += countInChapter;
                }
            }
        }

        if (totalOccurrences == 0) {
            if (publishedChapters.size() >= 3 && isMajorCharacter(ch)) {
                alerts.add(createAlert(
                        "character",
                        "主角「" + ch.getName() + "」尚未登场",
                        "当前已发布 " + publishedChapters.size() + " 章，主角「" + ch.getName() + "」仍未出场，建议在近期章节中引入",
                        "warning",
                        "考虑在下一章安排该角色的首次登场，或通过其他角色对话提及"
                ));
            }
            return;
        }

        if (firstThreeChaptersCount >= 3) {
            return;
        }

        if (!isMajorCharacter(ch)) {
            int absentChapters = currentChapter - lastSeenChapter;
            int threshold = supportingThreshold;
            if (absentChapters > threshold) {
                alerts.add(createAlert(
                        "character",
                        "角色「" + ch.getName() + "」长期未出场",
                        ch.getName() + "已连续 " + absentChapters + " 章未出场（上次出场于第 " + lastSeenChapter + " 章）",
                        "info",
                        "建议在后续章节中安排该角色回归，或交代其动向避免读者遗忘"
                ));
            }
            return;
        }

        int absentChapters = currentChapter - lastSeenChapter;
        if (absentChapters > protagonistThreshold) {
            alerts.add(createAlert(
                    "character",
                    "角色「" + ch.getName() + "」长期未出场",
                    ch.getName() + "已连续 " + absentChapters + " 章未出场（上次出场于第 " + lastSeenChapter + " 章）"
                            + "，该角色为" + getRoleLabel(ch.getRole()),
                    absentChapters > protagonistThreshold * 2 ? "warning" : "info",
                    "建议在后续章节中安排该角色回归，或交代其动向避免读者遗忘"
            ));
        }
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        while ((index = lowerText.indexOf(lowerKeyword, index)) != -1) {
            count++;
            index += lowerKeyword.length();
        }
        return count;
    }

    private void checkArcStagnation(NovelCharacter ch, List<NovelSentinelAlert> alerts) {
        // 只有设定了弧光终点但进度偏低的角色才触发
        if (ch.getArcEnd() == null || ch.getArcEnd().isEmpty()) return;
        if (ch.getArcProgress() == null) return;

        if (ch.getArcProgress() < arcStallThreshold) {
            alerts.add(createAlert(
                    "character",
                    "角色「" + ch.getName() + "」弧光进度停滞",
                    "弧光目标「" + ch.getArcEnd() + "」，当前进度仅 " + ch.getArcProgress() + "%"
                            + "，属于" + getRoleLabel(ch.getRole()),
                    ch.getArcProgress() < 10 ? "warning" : "info",
                    "建议推动该角色的成长弧线：可安排关键事件、内心冲突或关系变化来促进角色发展"
            ));
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