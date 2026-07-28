package com.novelcraft.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.novelcraft.web.common.BusinessException;
import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelChapterVersion;
import com.novelcraft.web.entity.NovelProject;
import com.novelcraft.web.entity.NovelVolume;
import com.novelcraft.web.entity.NovelWritingLog;
import com.novelcraft.web.mapper.NovelChapterMapper;
import com.novelcraft.web.mapper.NovelChapterVersionMapper;
import com.novelcraft.web.mapper.NovelProjectMapper;
import com.novelcraft.web.mapper.NovelVolumeMapper;
import com.novelcraft.web.mapper.NovelWritingLogMapper;
import com.novelcraft.web.service.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final NovelChapterMapper chapterMapper;
    private final NovelChapterVersionMapper versionMapper;
    private final NovelVolumeMapper volumeMapper;
    private final NovelProjectMapper projectMapper;
    private final NovelWritingLogMapper writingLogMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final DashboardCacheService dashboardCacheService;

    private static final java.time.ZoneId ZONE_CN = java.time.ZoneId.of("Asia/Shanghai");

    public List<NovelVolume> listVolumes(Long projectId) {
        return volumeMapper.selectByProjectId(projectId);
    }

    public NovelVolume createVolume(NovelVolume volume) {
        volumeMapper.insert(volume);
        return volume;
    }

    public List<NovelChapter> listChapters(Long projectId) {
        return chapterMapper.selectByProjectId(projectId);
    }

    public NovelChapter getChapter(Long id) {
        NovelChapter chapter = chapterMapper.selectByIdWithDeleted(id);
        if (chapter == null) {
            throw new BusinessException(404, "章节不存在");
        }
        return chapter;
    }

    @Transactional
    public NovelChapter createChapter(NovelChapter chapter) {
        // ✅ 如果没有设置 sortOrder，自动计算下一个序号
        if (chapter.getSortOrder() == null || chapter.getSortOrder() == 0) {
            Integer maxOrder = chapterMapper.getMaxSortOrder(chapter.getProjectId());
            chapter.setSortOrder(maxOrder != null ? maxOrder + 1 : 1);
        }
        chapterMapper.insert(chapter);
        updateProjectStats(chapter.getProjectId());
        dashboardCacheService.invalidate(chapter.getProjectId());
        recordWritingLog(chapter.getProjectId(), chapter.getId(), chapter.getWordCount(), 0);
        return chapter;
    }

    @Transactional
    public NovelChapter updateChapter(NovelChapter chapter) {
        return updateChapter(chapter, false);
    }

    @Transactional
    public NovelChapter updateChapter(NovelChapter chapter, boolean silent) {
        // 用显式 SQL 检查存在性（确保与 listChapters 口径一致）
        NovelChapter exist = chapterMapper.selectByIdWithDeleted(chapter.getId());
        if (exist == null) {
            throw new BusinessException(404, "章节不存在");
        }
        // 仅内容变化且非静默保存时才创建历史版本
        boolean contentChanged = chapter.getContent() != null
                && !chapter.getContent().equals(exist.getContent());
        if (contentChanged && !silent) {
            NovelChapterVersion version = new NovelChapterVersion();
            version.setChapterId(exist.getId());
            version.setContent(exist.getContent());
            version.setWordCount(exist.getWordCount());
            version.setVersion(exist.getVersion());
            version.setChangeNote("自动保存");
            versionMapper.insert(version);
        }
        // 递增版本号（仅内容变化且非静默时）
        if (contentChanged && !silent && (chapter.getVersion() == null || chapter.getVersion().isEmpty())) {
            String oldVer = exist.getVersion();
            if (oldVer != null && !oldVer.isEmpty()) {
                String digits = oldVer.replaceAll("\\D+", "");
                if (!digits.isEmpty()) {
                    int nextNum = Integer.parseInt(digits) + 1;
                    chapter.setVersion(oldVer.replaceFirst("\\d+", String.valueOf(nextNum)));
                }
            }
        }
        // 用 MyBatis-Plus updateById：仅更新非 null 字段
        chapterMapper.updateById(chapter);
        // 记录写作日志（热力图数据源）
        recordWritingLog(exist.getProjectId(), chapter.getId(), chapter.getWordCount(), exist.getWordCount());
        // 同步更新项目统计
        updateProjectStats(exist.getProjectId());
        dashboardCacheService.invalidate(exist.getProjectId());
        return chapterMapper.selectByIdWithDeleted(chapter.getId());
    }

    @Transactional
    public void deleteChapter(Long id) {
        // 先查出 projectId 用于后续统计更新
        NovelChapter chapter = chapterMapper.selectByIdWithDeleted(id);
        Long projectId = chapter != null ? chapter.getProjectId() : null;
        int affected = chapterMapper.markDeletedById(id);
        if (affected == 0) {
            throw new BusinessException(404, "章节不存在");
        }
        // 同步更新项目统计
        if (projectId != null) {
            updateProjectStats(projectId);
            dashboardCacheService.invalidate(projectId);
        }
    }

    /**
     * 重新计算项目字数与章节数，更新 novel_project 表
     * 使用 JdbcTemplate 直接 SQL，绕过 MyBatis-Plus @TableLogic 干扰
     */
    private void updateProjectStats(Long projectId) {
        try {
            int chapterCount = chapterMapper.countByProject(projectId);
            int totalWords = chapterMapper.sumWordCountByProject(projectId);
            jdbcTemplate.update(
                "UPDATE novel_project SET word_count = ?, chapter_count = ? WHERE id = ?",
                totalWords, chapterCount, projectId);
            log.info("项目 {} 统计已同步：{} 章，{} 字", projectId, chapterCount, totalWords);
        } catch (Exception e) {
            log.warn("更新项目 {} 统计失败：{}", projectId, e.getMessage());
        }
    }

    /**
     * 记录写作日志（热力图数据源）
     * 同一天多次保存时，记录增量字数变化
     */
    private void recordWritingLog(Long projectId, Long chapterId, Integer newWordCount, Integer oldWordCount) {
        if (projectId == null || newWordCount == null) return;
        try {
            // 计算增量字数（只计正增长）
            int delta = newWordCount;
            if (oldWordCount != null && oldWordCount > 0) {
                delta = Math.max(0, newWordCount - oldWordCount);
            }
            if (delta == 0) return;

            LocalDate today = LocalDate.now(ZONE_CN);
            NovelWritingLog exist = writingLogMapper.selectOne(
                    new LambdaQueryWrapper<NovelWritingLog>()
                            .eq(l -> l.getProjectId(), projectId)
                            .eq(l -> l.getDate(), today)
                            .eq(l -> l.getChapterId(), chapterId)
                            .last("LIMIT 1"));
            if (exist != null) {
                // 同一天同一章已记录，累加增量字数
                exist.setWordCount((exist.getWordCount() != null ? exist.getWordCount() : 0) + delta);
                writingLogMapper.updateById(exist);
            } else {
                NovelWritingLog log = new NovelWritingLog();
                log.setProjectId(projectId);
                log.setChapterId(chapterId);
                log.setDate(today);
                log.setWordCount(delta);
                log.setWritingDuration(30);
                writingLogMapper.insert(log);
            }
        } catch (Exception e) {
            log.warn("记录写作日志失败 (projectId={}, chapterId={}): {}", projectId, chapterId, e.getMessage());
        }
    }

    /**
     * 修复项目的所有章节 sortOrder
     * 按当前 id 排序重新分配连续的序号
     */
    @Transactional
    public int fixSortOrder(Long projectId) {
        List<NovelChapter> chapters = chapterMapper.selectList(
            new LambdaQueryWrapper<NovelChapter>()
                .eq(NovelChapter::getProjectId, projectId)
                .eq(NovelChapter::getDeleted, 0)
                .orderByAsc(NovelChapter::getId)
        );
        if (chapters.isEmpty()) return 0;

        int order = 1;
        for (NovelChapter ch : chapters) {
            ch.setSortOrder(order++);
            chapterMapper.updateById(ch);
        }
        log.info("修复项目 {} 的章节排序：共 {} 章", projectId, chapters.size());
        return chapters.size();
    }

    public List<NovelChapterVersion> listVersions(Long chapterId) {
        return versionMapper.selectByChapterId(chapterId);
    }
}