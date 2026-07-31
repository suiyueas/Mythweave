package com.novelcraft.web.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novelcraft.web.common.BusinessException;
import com.novelcraft.web.entity.NovelProject;
import com.novelcraft.web.mapper.NovelChapterMapper;
import com.novelcraft.web.mapper.NovelProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final JdbcTemplate jdbcTemplate;

    public NovelProject create(NovelProject project) {
        projectMapper.insert(project);
        // 返回数据库真实记录（含默认值 status= draft、时间等），避免前端拿到请求体空值误判状态
        return projectMapper.selectById(project.getId());
    }

    public NovelProject update(NovelProject project) {
        NovelProject exist = projectMapper.selectById(project.getId());
        if (exist == null) {
            throw new BusinessException(404, "作品不存在");
        }
        projectMapper.updateById(project);
        return projectMapper.selectById(project.getId());
    }

    @Transactional
    public void delete(Long id) {
        NovelProject exist = projectMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "作品不存在");
        }

        log.info("=== 开始硬删除作品 ID: {}, 名称: {} ===", id, exist.getTitle());

        try {
            // 1. 章节版本（通过子查询）
            int cnt1 = deleteIfExists("novel_chapter_version",
                "chapter_id IN (SELECT id FROM novel_chapter WHERE project_id = ?)", id);
            log.info("[ 1/17] novel_chapter_version 删除 {} 条", cnt1);

            // 2. 章节
            int cnt2 = deleteIfExists("novel_chapter", "project_id = ?", id);
            log.info("[ 2/17] novel_chapter 删除 {} 条", cnt2);

            // 3. 卷
            int cnt3 = deleteIfExists("novel_volume", "project_id = ?", id);
            log.info("[ 3/17] novel_volume 删除 {} 条", cnt3);

            // 4. 人物（人物关系表已废弃，直接删人物）
            int cnt4 = deleteIfExists("novel_character", "project_id = ?", id);
            log.info("[ 4/17] novel_character 删除 {} 条", cnt4);

            // 5. 世界观
            int cnt5 = deleteIfExists("novel_world_setting", "project_id = ?", id);
            log.info("[ 5/17] novel_world_setting 删除 {} 条", cnt5);

            // 6. 大纲
            int cnt6 = deleteIfExists("novel_outline", "project_id = ?", id);
            log.info("[ 6/17] novel_outline 删除 {} 条", cnt6);

            // 7. 情节线
            int cnt7 = deleteIfExists("novel_plot_thread", "project_id = ?", id);
            log.info("[ 7/17] novel_plot_thread 删除 {} 条", cnt7);

            // 7b. 情节知识图谱
            int cnt7b = deleteIfExists("novel_plot_knowledge_graph", "project_id = ?", id);
            log.info("[7b/17] novel_plot_knowledge_graph 删除 {} 条", cnt7b);

            // 8. 伏笔
            int cnt8 = deleteIfExists("novel_foreshadowing", "project_id = ?", id);
            log.info("[ 8/17] novel_foreshadowing 删除 {} 条", cnt8);

            // 9. 灵感
            int cnt9 = deleteIfExists("novel_inspiration", "project_id = ?", id);
            log.info("[ 9/17] novel_inspiration 删除 {} 条", cnt9);

            // 10. 上下文快照
            int cnt10 = deleteIfExists("novel_context_snapshot", "project_id = ?", id);
            log.info("[10/17] novel_context_snapshot 删除 {} 条", cnt10);

            // 11. AI 配置
            int cnt11 = deleteIfExists("novel_ai_config", "project_id = ?", id);
            log.info("[11/17] novel_ai_config 删除 {} 条", cnt11);

            // 12. AI 会话
            int cnt12 = deleteIfExists("novel_ai_session", "project_id = ?", id);
            log.info("[12/17] novel_ai_session 删除 {} 条", cnt12);

            // 13. 写作日志
            int cnt13 = deleteIfExists("novel_writing_log", "project_id = ?", id);
            log.info("[13/17] novel_writing_log 删除 {} 条", cnt13);

            // 14. 哨兵告警
            int cnt14 = deleteIfExists("novel_sentinel_alert", "project_id = ?", id);
            log.info("[14/17] novel_sentinel_alert 删除 {} 条", cnt14);

            // 14b. 哨兵巡查日志
            int cnt14b = deleteIfExists("novel_sentinel_check_log", "project_id = ?", id);
            log.info("[14b/17] novel_sentinel_check_log 删除 {} 条", cnt14b);

            // 15. 物理删除作品
            int cnt15 = projectMapper.deletePhysically(id);
            log.info("[15/17] novel_project 物理删除 {} 条", cnt15);

            if (cnt15 == 0) {
                throw new RuntimeException("作品物理删除失败（影响行数为0），可能已被其他操作删除");
            }

            log.info("=== 作品 {} 硬删除完成 ===", id);

        } catch (Exception e) {
            log.error("删除作品 {} 失败，事务将回滚: {}", id, e.getMessage(), e);
            throw new RuntimeException("删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * 表存在则按条件删除，表不存在（环境差异）则跳过，避免删除作品失败
     * whereClause 为完整 WHERE 条件（含占位符），args 为对应参数
     */
    private int deleteIfExists(String table, String whereClause, Object... args) {
        if (!tableExists(table)) {
            log.warn("[跳过] 表 {} 不存在，跳过删除", table);
            return 0;
        }
        return jdbcTemplate.update("DELETE FROM " + table + " WHERE " + whereClause, args);
    }

    private boolean tableExists(String tableName) {
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, tableName);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            log.warn("检查表 {} 是否存在失败: {}", tableName, e.getMessage());
            return true; // 查询失败时默认尝试删除
        }
    }

    public NovelProject getById(Long id) {
        NovelProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(404, "作品不存在");
        }
        return project;
    }

    public List<NovelProject> listByUserId(Long userId) {
        return projectMapper.selectByUserId(userId);
    }

    public IPage<NovelProject> page(int pageNum, int pageSize) {
        Page<NovelProject> page = new Page<>(pageNum, pageSize);
        return projectMapper.selectPage(page, null);
    }

    /**
     * 同步单个作品的字数与章节数统计（从 novel_chapter 实际数据重算）
     * 使用 JdbcTemplate 直接 SQL，绕过 MyBatis-Plus @TableLogic 干扰
     */
    public void syncProjectStats(Long projectId) {
        Integer wordCount = chapterMapper.sumWordCountByProject(projectId);
        Integer chapterCount = chapterMapper.countByProject(projectId);
        int wc = wordCount != null ? wordCount : 0;
        int cc = chapterCount != null ? chapterCount : 0;
        log.info("syncProjectStats: 项目 {} → {} 章, {} 字", projectId, cc, wc);
        // 直接用 JdbcTemplate 执行 UPDATE，绕过 MyBatis-Plus 的 @TableLogic
        jdbcTemplate.update(
            "UPDATE novel_project SET word_count = ?, chapter_count = ? WHERE id = ?",
            wc, cc, projectId);
    }

    /**
     * 同步当前用户所有作品的统计（修复历史数据不一致）
     */
    public int syncAllProjectStats(Long userId) {
        List<NovelProject> projects = projectMapper.selectByUserId(userId);
        log.info("syncAllProjectStats: 用户 {} 有 {} 个项目", userId, projects.size());
        int count = 0;
        for (NovelProject p : projects) {
            count++; // 先计数，确保返回正确的项目数
            try {
                syncProjectStats(p.getId());
                backfillWritingLogs(p.getId());
            } catch (Exception e) {
                log.warn("项目 {}({}) 统计同步失败：{}", p.getTitle(), p.getId(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 回填缺失的写作日志（热力图/本周趋势/最近活动的数据源）
     * 为没有写作日志的章节自动创建一条记录
     */
    private void backfillWritingLogs(Long projectId) {
        try {
            // 查询有字数但无对应写作日志的章节
            List<Map<String, Object>> chapters = jdbcTemplate.queryForList(
                "SELECT c.id, c.word_count, c.create_time FROM novel_chapter c " +
                "WHERE c.project_id = ? AND c.deleted = 0 AND c.word_count > 0 " +
                "AND NOT EXISTS ("
                + "  SELECT 1 FROM novel_writing_log w "
                + "  WHERE w.project_id = c.project_id AND w.chapter_id = c.id AND w.deleted = 0"
                + ")", projectId);

            if (chapters.isEmpty()) return;

            int inserted = 0;
            for (Map<String, Object> ch : chapters) {
                Number chId = (Number) ch.get("id");
                Number wc = (Number) ch.get("word_count");
                Object createTime = ch.get("create_time");

                if (chId == null || wc == null || wc.intValue() <= 0) continue;

                // 取章节创建时间的日期部分
                LocalDateTime dt = null;
                if (createTime instanceof LocalDateTime) {
                    dt = (LocalDateTime) createTime;
                } else if (createTime instanceof Date) {
                    dt = LocalDateTime.ofInstant(((Date) createTime).toInstant(), ZoneId.systemDefault());
                }
                if (dt == null) dt = LocalDateTime.now();

                jdbcTemplate.update(
                    "INSERT INTO novel_writing_log (project_id, chapter_id, date, word_count, writing_duration, create_time, update_time, deleted) " +
                    "VALUES (?, ?, DATE(?), ?, 30, ?, ?, 0)",
                    projectId, chId.longValue(), dt, wc.intValue(), dt, dt);
                inserted++;
            }
            if (inserted > 0) {
                log.info("项目 {} 回填 {} 条写作日志", projectId, inserted);
            }
        } catch (Exception e) {
            log.warn("项目 {} 回填写作日志失败：{}", projectId, e.getMessage());
        }
    }
}