package com.novelcraft.web.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "novel_analysis")
public class NovelAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "chapter_title")
    private String chapterTitle;

    @Column(name = "chapter_index")
    private Integer chapterIndex;

    @Column(name = "editor_result", columnDefinition = "TEXT")
    private String editorResult;

    @Column(name = "character_result", columnDefinition = "TEXT")
    private String characterResult;

    @Column(name = "style_result", columnDefinition = "TEXT")
    private String styleResult;

    @Column(name = "reader_result", columnDefinition = "TEXT")
    private String readerResult;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_cost_ms")
    private Long totalCostMs;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}