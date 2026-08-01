package com.novelcraft.web.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.novelcraft.web.common.BusinessException;
import com.novelcraft.web.entity.NovelChapter;
import com.novelcraft.web.entity.NovelChapterVersion;
import com.novelcraft.web.entity.NovelProject;
import com.novelcraft.web.entity.NovelWritingLog;
import com.novelcraft.web.mapper.NovelChapterMapper;
import com.novelcraft.web.mapper.NovelChapterVersionMapper;
import com.novelcraft.web.mapper.NovelForeshadowingMapper;
import com.novelcraft.web.mapper.NovelProjectMapper;
import com.novelcraft.web.mapper.NovelWritingLogMapper;
import com.novelcraft.web.service.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private final NovelProjectMapper projectMapper;
    private final NovelWritingLogMapper writingLogMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final DashboardCacheService dashboardCacheService;
    private final SentinelService sentinelService;

    @Value("${app.sentinel.auto-check-enabled:true}")
    private boolean autoCheckEnabled;

    private static final java.time.ZoneId ZONE_CN = java.time.ZoneId.of("Asia/Shanghai");

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
        triggerChapterCheck(chapter);
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
        boolean contentChanged = isContentChanged(chapter.getContent(), exist.getContent());
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
        // 用 MyBatis-Plus updateById：仅更新非 null 字段（但 updateTime 会被自动更新）
        // 优化：仅在内容或标题实际变化时才更新 updateTime，避免"查看就更新时间"
        if (contentChanged || isTitleChanged(chapter, exist)) {
            chapter.setUpdateTime(LocalDateTime.now());
            chapterMapper.updateById(chapter);
        }
        // 记录写作日志（热力图数据源）
        recordWritingLog(exist.getProjectId(), chapter.getId(), chapter.getWordCount(), exist.getWordCount());
        // 内容被清空时：该章节作为回收章节的伏笔其回收内容已消失，即时回退为待回收
        if (contentChanged && chapter.getContent() != null && chapter.getContent().trim().isEmpty()) {
            try {
                int reverted = foreshadowingMapper.revertResolvedByChapter(exist.getProjectId(), chapter.getId());
                if (reverted > 0) {
                    log.info("章节{}内容清空，回退已回收伏笔{}条", chapter.getId(), reverted);
                }
            } catch (Exception e) {
                log.warn("清空内容后伏笔回退失败: {}", e.getMessage());
            }
        }
        // 同步更新项目统计
        updateProjectStats(exist.getProjectId());
        try {
            dashboardCacheService.invalidate(exist.getProjectId());
        } catch (Exception e) {
            log.warn("更新章节后缓存失效失败: {}", e.getMessage());
        }
        // 内容变化时触发章节检查
        if (contentChanged) {
            triggerChapterCheck(chapterMapper.selectByIdWithDeleted(chapter.getId()));
        }
        return chapterMapper.selectByIdWithDeleted(chapter.getId());
    }

    private boolean isTitleChanged(NovelChapter newChapter, NovelChapter exist) {
        return newChapter.getTitle() != null && !newChapter.getTitle().equals(exist.getTitle());
    }

    private boolean isContentChanged(String newContent, String existContent) {
        // null 表示前端未传 content（不更新）；空字符串表示用户清空内容（视为真实变化）
        if (newContent == null && existContent == null) return false;
        String a = newContent != null ? newContent : "";
        String b = existContent != null ? existContent : "";
        return !a.equals(b);
    }

    @Transactional
    public void deleteChapter(Long id) {
        // 先查出 projectId 用于后续统计更新
        NovelChapter chapter = chapterMapper.selectByIdWithDeleted(id);
        Long projectId = chapter != null ? chapter.getProjectId() : null;
        int affected = chapterMapper.markDeletedById(id);
        if (affected == 0) {
            // 幂等：章节已处于删除状态（deleted=1）视为删除成功，仅真正不存在的才报 404
            if (chapter == null) {
                throw new BusinessException(404, "章节不存在");
            }
            log.info("章节 {} 已被删除过，幂等返回", id);
        }
        // 联动伏笔：回收章节被删除 → 伏笔回收未完成，回退为待回收；
        // 并自愈历史错误数据（resolved_chapter_id 误存 project_id 的孤儿标记）
        if (projectId != null) {
            try {
                int reverted = foreshadowingMapper.revertResolvedByChapter(projectId, id);
                int healed = foreshadowingMapper.healOrphanResolved(projectId);
                log.info("删除章节后伏笔联动: 章节={}, 直接回退{}条, 自愈{}条", id, reverted, healed);
            } catch (Exception e) {
                log.warn("删除章节后伏笔联动处理失败: {}", e.getMessage());
            }
        }
        // 同步更新项目统计（各步骤独立容错，避免异常导致删除事务回滚）
        if (projectId != null) {
            updateProjectStats(projectId);
            try {
                dashboardCacheService.invalidate(projectId);
            } catch (Exception e) {
                log.warn("删除章节后缓存失效失败: {}", e.getMessage());
            }
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

    /** 版本列表最大返回条数（防止版本无限增长拖慢查询） */
    private static final int VERSION_LIST_LIMIT = 100;

    /**
     * 查询章节版本摘要列表（不含 content 大字段，按 id 倒序，走索引避免 filesort）
     */
    public List<NovelChapterVersion> listVersions(Long chapterId) {
        return versionMapper.selectSummaryByChapterId(chapterId, VERSION_LIST_LIMIT);
    }

    /**
     * 查询单个版本详情（含 content，用于预览/恢复）
     * 校验版本归属章节，防止越权访问其他章节的版本
     */
    public NovelChapterVersion getVersionDetail(Long chapterId, Long versionId) {
        NovelChapterVersion version = versionMapper.selectDetailById(versionId);
        if (version == null) {
            throw new BusinessException(404, "版本不存在");
        }
        if (!chapterId.equals(version.getChapterId())) {
            throw new BusinessException(403, "版本不属于该章节");
        }
        return version;
    }

    /**
     * 异步触发章节内容检查（智能哨兵实时检测）
     * 仅在内容变化时触发，异步执行不阻塞主业务流程
     */
    @Async("sentinelTaskExecutor")
    public void triggerChapterCheck(NovelChapter chapter) {
        if (chapter == null) return;
        if (!autoCheckEnabled) {
            log.debug("章节自动检测已禁用，跳过章节 {}", chapter.getId());
            return;
        }
        try {
            sentinelService.saveChapterAlerts(chapter);
            log.info("章节 {} 自动检测完成", chapter.getId());
        } catch (Exception e) {
            log.error("章节 {} 自动检测失败: {}", chapter.getId(), e.getMessage(), e);
        }
    }
}