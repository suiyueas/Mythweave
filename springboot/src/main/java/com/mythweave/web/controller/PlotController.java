package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.entity.*;
import com.mythweave.web.mapper.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 情节管理控制器（情节引擎）
 * 
 * 主要功能：
 * - 情节线（PlotThread）的CRUD管理
 * - 伏笔（Foreshadowing）的CRUD管理
 * - 伏笔紧急程度查询（基于当前章节位置）
 * - 孤儿伏笔自愈回收机制
 * 
 * 情节线用于管理作品的主要故事线索
 * 伏笔用于记录埋下的伏笔及其回收状态
 * 
 * 所有接口都需要用户登录认证
 */
@Slf4j
@Tag(name = "情节引擎")
@RestController
@RequestMapping("/api/projects/{projectId}/plot")
@RequiredArgsConstructor
public class PlotController {
    
    private final NovelPlotThreadMapper threadMapper;
    private final NovelForeshadowingMapper foreshadowingMapper;

    // ═══ 情节线 CRUD ═══

    /** 获取作品的所有情节线 */
    @GetMapping("/threads") public R<List<NovelPlotThread>> listThreads(@PathVariable Long projectId) { return R.ok(threadMapper.selectByProjectId(projectId)); }
    
    /** 创建新的情节线 */
    @PostMapping("/threads") public R<NovelPlotThread> createThread(@PathVariable Long projectId, @Valid @RequestBody NovelPlotThread t) { t.setProjectId(projectId); threadMapper.insert(t); return R.ok(t); }
    
    /** 更新情节线信息 */
    @PutMapping("/threads/{id}") public R<NovelPlotThread> updateThread(@PathVariable Long id, @RequestBody NovelPlotThread t) { t.setId(id); threadMapper.updateById(t); return R.ok(threadMapper.selectById(id)); }
    
    /** 删除情节线 */
    @DeleteMapping("/threads/{id}") public R<Void> deleteThread(@PathVariable Long id) { threadMapper.deleteById(id); return R.ok(); }

    // ═══ 伏笔管理 ═══

    /**
     * 获取作品的所有伏笔列表
     * 自动触发孤儿伏笔自愈回收机制
     */
    @GetMapping("/foreshadowing") public R<List<NovelForeshadowing>> listForeshadowing(@PathVariable Long projectId) {
        healOrphanForeshadowing(projectId);
        return R.ok(foreshadowingMapper.selectByProjectId(projectId));
    }

    /**
     * 获取紧急需要回收的伏笔列表
     * 
     * @param projectId 作品ID
     * @param currentChapter 当前章节号（伏笔超过此章节未回收会被标记为紧急）
     * @return 紧急伏笔列表
     */
    @GetMapping("/foreshadowing/urgent") public R<List<NovelForeshadowing>> listUrgentForeshadowing(@PathVariable Long projectId, @RequestParam(defaultValue = "999") Integer currentChapter) {
        healOrphanForeshadowing(projectId);
        return R.ok(foreshadowingMapper.selectUrgentByProject(projectId, currentChapter));
    }

    /** 创建新的伏笔 */
    @PostMapping("/foreshadowing") public R<NovelForeshadowing> createForeshadowing(@PathVariable Long projectId, @Valid @RequestBody NovelForeshadowing f) { f.setProjectId(projectId); foreshadowingMapper.insert(f); return R.ok(f); }
    
    /** 更新伏笔信息 */
    @PutMapping("/foreshadowing/{id}") public R<NovelForeshadowing> updateForeshadowing(@PathVariable Long id, @RequestBody NovelForeshadowing f) { f.setId(id); foreshadowingMapper.updateById(f); return R.ok(foreshadowingMapper.selectById(id)); }

    /**
     * 孤儿伏笔自愈回收机制
     * 
     * 回收章节不存在（含历史误存project_id的数据）的伏笔
     * 将其回退为待回收状态，避免无效数据影响统计
     */
    private void healOrphanForeshadowing(Long projectId) {
        try {
            int healed = foreshadowingMapper.healOrphanResolved(projectId);
            if (healed > 0) {
                log.info("伏笔自愈: projectId={}, 回退{}条孤儿回收标记", projectId, healed);
            }
        } catch (Exception e) {
            log.warn("伏笔自愈失败: {}", e.getMessage());
        }
    }
}