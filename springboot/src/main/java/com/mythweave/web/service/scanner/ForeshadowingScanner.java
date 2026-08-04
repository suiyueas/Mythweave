package com.mythweave.web.service.scanner;

import com.mythweave.web.entity.NovelForeshadowing;
import com.mythweave.web.entity.NovelSentinelAlert;
import com.mythweave.web.mapper.NovelChapterMapper;
import com.mythweave.web.mapper.NovelForeshadowingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForeshadowingScanner {

    private final NovelForeshadowingMapper foreshadowingMapper;
    private final NovelChapterMapper chapterMapper;

    @Value("${app.sentinel.overdue-threshold:10}")
    private int overdueThreshold;

    public List<NovelSentinelAlert> scan(Long projectId) {
        List<NovelSentinelAlert> alerts = new ArrayList<>();

        List<NovelForeshadowing> pending = foreshadowingMapper.selectPendingByProject(projectId);
        Integer currentChapter = chapterMapper.selectMaxSortOrder(projectId);
        if (currentChapter == null) currentChapter = 0;

        for (NovelForeshadowing fs : pending) {
            int passed = currentChapter.intValue() - fs.getChapterId().intValue();
            fs.setPassedChapters(passed);
            foreshadowingMapper.updateById(fs);

            // 伏笔超期未回收告警
            if (passed > overdueThreshold) {
                alerts.add(createAlert(
                        "foreshadowing",
                        "伏笔超期未回收：" + fs.getName(),
                        "伏笔「" + fs.getName() + "」已埋设 " + passed + " 章，建议尽快回收",
                        "warning",
                        "建议在下一章节中呼应此伏笔"
                ));
            }

            // 伏笔即将回收提醒
            if (fs.getResolvedChapterId() != null) {
                int remaining = fs.getResolvedChapterId().intValue() - currentChapter.intValue();
                if (remaining > 0 && remaining <= 3) {
                    alerts.add(createAlert(
                            "foreshadowing",
                            "伏笔即将回收：" + fs.getName(),
                            "伏笔「" + fs.getName() + "」应在 CH." + fs.getResolvedChapterId() + " 回收，当前 CH." + currentChapter,
                            "info",
                            "请确保在 CH." + fs.getResolvedChapterId() + " 中呼应此伏笔"
                    ));
                }
            }
        }

        log.info("🔍 伏笔巡查完成，发现 {} 条告警", alerts.size());
        return alerts;
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
