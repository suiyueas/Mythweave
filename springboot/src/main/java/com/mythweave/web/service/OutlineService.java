package com.mythweave.web.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mythweave.web.entity.NovelOutline;
import com.mythweave.web.mapper.NovelOutlineMapper;
import com.mythweave.web.model.OutlineActDto;
import com.mythweave.web.model.OutlineNodeDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutlineService {
    private final NovelOutlineMapper outlineMapper;

    public List<NovelOutline> getOutlinesByProjectId(Long projectId) {
        return outlineMapper.selectByProjectId(projectId);
    }

    /**
     * 批量保存幕与节点（AI 生成的大纲整体保存，事务性重建）
     * 智能归类：先创建所有幕（卷）节点并记录 ID，再将章节节点挂到对应幕下。
     * 动态适应：幕数量由传入的 acts 决定，支持任意数量的幕。
     */
    @Transactional
    public int saveOutlineActs(Long projectId, List<OutlineActDto> acts) {
        if (acts == null || acts.isEmpty()) {
            return 0;
        }

        // 1. 删除该项目的旧大纲（逻辑删除，与其它模块保持一致）
        outlineMapper.delete(new QueryWrapper<NovelOutline>()
                .eq("project_id", projectId));

        // 2. 先插入所有幕（卷）节点，记录 act -> 幕ID 映射
        Map<String, Long> actIdMap = new LinkedHashMap<>();
        for (int i = 0; i < acts.size(); i++) {
            OutlineActDto actDto = acts.get(i);
            String actKey = normalizeActKey(actDto.getAct(), i);

            NovelOutline volume = new NovelOutline();
            volume.setProjectId(projectId);
            volume.setParentId(null);
            volume.setAct(actKey);
            volume.setTitle(actDto.getTitle() != null && !actDto.getTitle().isEmpty()
                    ? actDto.getTitle() : "第" + (i + 1) + "幕");
            volume.setDescription(actDto.getDescription());
            volume.setType("volume");
            volume.setNodeStatus("draft");
            volume.setNodeNumber(0);
            volume.setSortOrder(actDto.getSortOrder() != null ? actDto.getSortOrder() : i + 1);
            outlineMapper.insert(volume);
            actIdMap.put(actKey, volume.getId());
        }

        // 3. 插入所有章节节点，关联到对应的幕
        int savedNodes = 0;
        for (int i = 0; i < acts.size(); i++) {
            OutlineActDto actDto = acts.get(i);
            String actKey = normalizeActKey(actDto.getAct(), i);
            Long parentId = actIdMap.get(actKey);
            if (parentId == null) {
                // 容错：若未找到对应幕，挂到第一幕
                parentId = actIdMap.values().iterator().next();
            }

            List<OutlineNodeDto> nodes = actDto.getNodes();
            if (nodes == null || nodes.isEmpty()) {
                continue;
            }
            for (int j = 0; j < nodes.size(); j++) {
                OutlineNodeDto nodeDto = nodes.get(j);
                NovelOutline node = new NovelOutline();
                node.setProjectId(projectId);
                node.setParentId(parentId);
                node.setAct(actKey);
                node.setTitle(nodeDto.getTitle() != null ? nodeDto.getTitle() : "未命名节点");
                String desc = nodeDto.getDescription();
                if (nodeDto.getKeyEvent() != null && !nodeDto.getKeyEvent().isEmpty()) {
                    desc = (desc != null ? desc : "") + "\n关键事件：" + nodeDto.getKeyEvent();
                }
                node.setDescription(desc);
                node.setType(nodeDto.getType() != null ? nodeDto.getType() : "chapter");
                node.setNodeStatus(nodeDto.getStatus() != null ? nodeDto.getStatus() : "draft");
                node.setSortOrder(nodeDto.getSortOrder() != null ? nodeDto.getSortOrder() : j + 1);
                node.setNodeNumber(j + 1);
                node.setEstimatedWords(nodeDto.getEstimatedWords());
                outlineMapper.insert(node);
                savedNodes++;
            }
        }

        log.info("📋 批量保存大纲完成 projectId={}, 幕数={}, 章节点数={}", projectId, actIdMap.size(), savedNodes);
        return savedNodes;
    }

    /**
     * 修复幕区分布：
     * 1. 若项目缺少幕（卷）节点，先按现有节点分布自动创建三幕并建立 parentId 关联；
     * 2. 将所有章节节点按 sortOrder 重新均匀分配到各幕，同步 act / nodeNumber / 幕内排序。
     */
    @Transactional
    public int fixActDistribution(Long projectId) {
        List<NovelOutline> outlines = outlineMapper.selectByProjectId(projectId);
        if (outlines == null || outlines.isEmpty()) {
            return 0;
        }

        // 已有幕节点：仅修复章节节点的归属
        List<NovelOutline> volumes = new ArrayList<>(outlines.stream()
                .filter(o -> "volume".equals(o.getType()))
                .sorted((a, b) -> Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .toList());

        List<NovelOutline> chapters;
        if (volumes.isEmpty()) {
            // 旧数据修复：自动创建三幕节点并挂接章节
            chapters = outlines.stream()
                    .filter(o -> !"volume".equals(o.getType()))
                    .sorted((a, b) -> Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                            b.getSortOrder() != null ? b.getSortOrder() : 0))
                    .toList();
            if (chapters.isEmpty()) {
                return 0;
            }
            String[] actKeys = {"first_act", "second_act", "third_act"};
            String[] actTitles = {"第一幕：建置", "第二幕：对抗", "第三幕：解决"};
            int total = chapters.size();
            int perAct = Math.max(1, (int) Math.ceil(total / 3.0));
            for (int i = 0; i < actKeys.length; i++) {
                NovelOutline volume = new NovelOutline();
                volume.setProjectId(projectId);
                volume.setParentId(null);
                volume.setAct(actKeys[i]);
                volume.setTitle(actTitles[i]);
                volume.setType("volume");
                volume.setNodeStatus("draft");
                volume.setSortOrder(i + 1);
                outlineMapper.insert(volume);
                volumes.add(volume);
            }

            int actIndex = 0;
            int actCounter = 0;
            int globalSort = 0;
            for (NovelOutline chapter : chapters) {
                if (actIndex < 2 && actCounter >= perAct) {
                    actIndex++;
                    actCounter = 0;
                }
                chapter.setParentId(volumes.get(actIndex).getId());
                chapter.setAct(actKeys[actIndex]);
                chapter.setNodeNumber(++actCounter);
                chapter.setSortOrder(++globalSort);
                outlineMapper.updateById(chapter);
            }
            return chapters.size();
        }

        // 已有幕节点：按全局 sortOrder 均匀分配到各幕（连续块分配，动态适配幕数）
        List<NovelOutline> unbound = outlines.stream()
                .filter(o -> !"volume".equals(o.getType()))
                .filter(o -> o.getParentId() == null)
                .sorted((a, b) -> Integer.compare(a.getSortOrder() != null ? a.getSortOrder() : 0,
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .toList();
        if (unbound.isEmpty()) {
            return 0;
        }
        int total = unbound.size();
        int perAct = Math.max(1, (int) Math.ceil(total / (double) volumes.size()));
        int actIndex = 0;
        int actCounter = 0;
        int globalSort = 0;
        int affected = 0;
        for (NovelOutline chapter : unbound) {
            if (actIndex < volumes.size() - 1 && actCounter >= perAct) {
                actIndex++;
                actCounter = 0;
            }
            NovelOutline volume = volumes.get(actIndex);
            chapter.setParentId(volume.getId());
            chapter.setAct(volume.getAct());
            chapter.setNodeNumber(++actCounter);
            chapter.setSortOrder(++globalSort);
            outlineMapper.updateById(chapter);
            affected++;
        }
        return affected;
    }

    /** 规范化幕标识：为空时自动生成 act_1 / act_2 ... */
    private String normalizeActKey(String act, int index) {
        if (act == null || act.isBlank()) {
            return "act_" + (index + 1);
        }
        return act.length() > 19 ? act.substring(0, 19) : act;
    }
}