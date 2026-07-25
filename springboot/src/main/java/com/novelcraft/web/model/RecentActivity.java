package com.novelcraft.web.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelWritingLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 最近活动
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivity {
    private String id;
    private String icon;
    private String title;
    private String desc;
    private String type;
    private Boolean highlight;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime time;

    public static RecentActivity ofChapter(NovelChapter ch) {
        return RecentActivity.builder()
                .id("ch-" + ch.getId())
                .icon(ch.getStatus() != null && ch.getStatus().contains("完成") ? "✅" : "📝")
                .title("章节更新")
                .desc(ch.getTitle() + " · " + (ch.getWordCount() != null ? ch.getWordCount() : 0) + "字")
                .type("chapter")
                .highlight(false)
                .time(ch.getUpdateTime())
                .build();
    }

    public static RecentActivity ofLog(NovelWritingLog log) {
        return RecentActivity.builder()
                .id("log-" + log.getId())
                .icon("✍️")
                .title("写作记录")
                .desc("写作 " + (log.getWordCount() != null ? log.getWordCount() : 0) + " 字 · "
                        + (log.getWritingDuration() != null ? log.getWritingDuration() : 0) + " 分钟")
                .type("writing")
                .highlight(log.getWordCount() != null && log.getWordCount() >= 3000)
                .time(log.getCreateTime())
                .build();
    }
}
