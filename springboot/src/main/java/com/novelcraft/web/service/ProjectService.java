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

/**
 * 作品项目管理服务层
 * 
 * 核心功能：
 * - 作品的创建、更新、删除、查询操作
 * - 作品删除时的级联处理（关联章节、角色、世界观等数据的统一删除）
 * - 作品统计数据同步（章节数、总字数）
 * - 写作日志自动回填（为没有日志的章节自动创建记录）
 * 
 * 使用事务管理确保数据一致性，级联删除失败时会回滚整个操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final NovelProjectMapper projectMapper;
    private final NovelChapterMapper chapterMapper;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建新作品
     * 插入后返回数据库中的完整记录（包含默认状态和时间戳），避免前端获取到空值
     * @param project 作品信息
     * @return 创建成功且包含数据库默认值的作品对象
     */
    public NovelProject create(NovelProject project) {
        projectMapper.insert(project);
        // 返回数据库真实记录（含默认值 status= draft、时间等），避免前端拿到请求体空值误判状态
        return projectMapper.selectById(project.getId());
    }

    /**
     * 更新作品信息
     * @param project 更新后的作品信息（需包含作品ID）
     * @return 更新后的作品对象
     * @throws BusinessException 当作品不存在时抛出404异常
     */
    public NovelProject update(NovelProject project) {
        NovelProject exist = projectMapper.selectById(project.getId());
        if (exist == null) {
            throw new BusinessException(404, "作品不存在");
        }
        projectMapper.updateById(project);
        return projectMapper.selectById(project.getId());
    }

    /**
     * 删除作品（硬删除，包含级联删除关联数据）
     * 
     * 级联删除顺序：
     * 1. 章节版本 (novel_chapter_version)
     * 2. 章节 (novel_chapter)
     * 3. 人物 (novel_character)
     * 4. 世界观 (novel_world_setting)
     * 5. 大纲 (novel_outline)
     * 6. 情节线 (novel_plot_thread)
     * 7. 伏笔 (novel_foreshadowing)
     * 8. 灵感 (novel_inspiration)
     * 9. AI配置 (novel_ai_config)
     * 10. AI会话 (novel_ai_session)
     * 11. 写作日志 (novel_writing_log)
     * 12. 哨兵告警 (novel_sentinel_alert)
     * 13. 哨兵巡查日志 (novel_sentinel_check_log)
     * 14. 最后删除作品本身 (novel_project)
     * 
     * 使用事务管理，任意一步失败都会回滚整个删除操作
     * @param id 要删除的作品ID
     * @throws BusinessException 当作品不存在时抛出404异常
     * @throws RuntimeException 当物理删除失败时抛出异常
     */
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
            log.info("[ 2/16] novel_chapter 删除 {} 条", cnt2);

            // 3. 人物（人物关系表已废弃，直接删人物）
            int cnt3 = deleteIfExists("novel_character", "project_id = ?", id);
            log.info("[ 3/16] novel_character 删除 {} 条", cnt3);

            // 4. 世界观
            int cnt4 = deleteIfExists("novel_world_setting", "project_id = ?", id);
            log.info("[ 4/16] novel_world_setting 删除 {} 条", cnt4);

            // 5. 大纲
            int cnt5 = deleteIfExists("novel_outline", "project_id = ?", id);
            log.info("[ 5/16] novel_outline 删除 {} 条", cnt5);

            // 6. 情节线
            int cnt6 = deleteIfExists("novel_plot_thread", "project_id = ?", id);
            log.info("[ 6/14] novel_plot_thread 删除 {} 条", cnt6);

            // 7. 伏笔
            int cnt7 = deleteIfExists("novel_foreshadowing", "project_id = ?", id);
            log.info("[ 7/14] novel_foreshadowing 删除 {} 条", cnt7);

            // 8. 灵感
            int cnt8 = deleteIfExists("novel_inspiration", "project_id = ?", id);
            log.info("[ 8/14] novel_inspiration 删除 {} 条", cnt8);

            // 9. AI 配置
            int cnt9 = deleteIfExists("novel_ai_config", "project_id = ?", id);
            log.info("[ 9/14] novel_ai_config 删除 {} 条", cnt9);

            // 10. AI 会话
            int cnt10 = deleteIfExists("novel_ai_session", "project_id = ?", id);
            log.info("[10/14] novel_ai_session 删除 {} 条", cnt10);

            // 11. 写作日志
            int cnt11 = deleteIfExists("novel_writing_log", "project_id = ?", id);
            log.info("[11/14] novel_writing_log 删除 {} 条", cnt11);

            // 12. 哨兵告警
            int cnt12 = deleteIfExists("novel_sentinel_alert", "project_id = ?", id);
            log.info("[12/14] novel_sentinel_alert 删除 {} 条", cnt12);

            // 13. 哨兵巡查日志
            int cnt13 = deleteIfExists("novel_sentinel_check_log", "project_id = ?", id);
            log.info("[13/14] novel_sentinel_check_log 删除 {} 条", cnt13);

            // 14. 物理删除作品
            int cnt14 = projectMapper.deletePhysically(id);
            log.info("[14/14] novel_project 物理删除 {} 条", cnt14);

            if (cnt14 == 0) {
                throw new RuntimeException("作品物理删除失败（影响行数为0），可能已被其他操作删除");
            }

            log.info("=== 作品 {} 硬删除完成 ===", id);

        } catch (Exception e) {
            log.error("删除作品 {} 失败，事务将回滚: {}", id, e.getMessage(), e);
            throw new RuntimeException("删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * 条件删除（表存在性检查）
     * 
     * 如果表不存在（环境差异导致），跳过删除操作而不是报错
     * 这样可以避免因环境差异导致删除作品失败
     * 
     * @param table 表名
     * @param whereClause 完整的WHERE条件（含占位符?）
     * @param args WHERE条件对应的参数
     * @return 删除的记录数
     */
    private int deleteIfExists(String table, String whereClause, Object... args) {
        if (!tableExists(table)) {
            log.warn("[跳过] 表 {} 不存在，跳过删除", table);
            return 0;
        }
        return jdbcTemplate.update("DELETE FROM " + table + " WHERE " + whereClause, args);
    }

    /**
     * 检查数据库表是否存在
     * 通过查询 information_schema.tables 实现
     * 
     * @param tableName 表名
     * @return 表存在返回true，否则返回false
     */
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

    /**
     * 根据ID获取作品详情
     * @param id 作品ID
     * @return 作品对象
     * @throws BusinessException 当作品不存在时抛出404异常
     */
    public NovelProject getById(Long id) {
        NovelProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(404, "作品不存在");
        }
        return project;
    }

    /**
     * 获取指定用户的所有作品列表
     * @param userId 用户ID
     * @return 用户作品列表
     */
    public List<NovelProject> listByUserId(Long userId) {
        return projectMapper.selectByUserId(userId);
    }

    /**
     * 分页获取所有作品列表
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页记录数
     * @return 分页结果
     */
    public IPage<NovelProject> page(int pageNum, int pageSize) {
        Page<NovelProject> page = new Page<>(pageNum, pageSize);
        return projectMapper.selectPage(page, null);
    }

    /**
     * 同步单个作品的统计数据
     * 
     * 从 novel_chapter 表中实时统计：
     * - word_count: 所有章节的总字数
     * - chapter_count: 有效章节数量
     * 
     * 使用 JdbcTemplate 直接执行 SQL，绕过 MyBatis-Plus 的 @TableLogic 逻辑删除干扰
     * 
     * @param projectId 作品ID
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
     * 同步当前用户所有作品的统计数据
     * 用于修复因历史操作导致的作品统计数据不一致问题
     * 同时会回填缺失的写作日志
     * 
     * @param userId 用户ID
     * @return 被成功同步统计的项目数量
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
     * 回填缺失的写作日志
     * 
     * 为有字数但没有对应写作日志的章节自动创建日志记录
     * 这是为了保证热力图、本周趋势、最近活动等功能的数据完整性
     * 
     * 查询逻辑：找出有字数且已创建但没有写作日志的章节
     * 插入时使用章节创建时间作为写作日期，默认写作时长30分钟
     * 
     * @param projectId 作品ID
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