package com.novelcraft.web.service;

import com.novelcraft.web.common.BusinessException;
import com.novelcraft.web.entity.NovelWorldSetting;
import com.novelcraft.web.mapper.NovelWorldSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldSettingService {

    private final NovelWorldSettingMapper settingMapper;

    /**
     * 分类统一映射：英文 ID → 中文显示名（与前端 store 的 CATEGORY_ID_TO_NAME 保持一致）。
     * 历史数据/各写入源可能使用英文 ID（如 geography、era），统一转为中文名，
     * 避免同一分类中英文混杂导致前端分类匹配失败。
     */
    private static final Map<String, String> CATEGORY_EN_TO_CN = Map.ofEntries(
            Map.entry("era", "时代背景"),
            Map.entry("geography", "地理版图"),
            Map.entry("history", "历史年表"),
            Map.entry("magic", "力量体系"),
            Map.entry("powerSystem", "力量体系"),
            Map.entry("magicSystem", "力量体系"),
            Map.entry("politics", "政治势力"),
            Map.entry("factions", "政治势力"),
            Map.entry("factionList", "政治势力"),
            Map.entry("culture", "文化社会"),
            Map.entry("technology", "科技文明"),
            Map.entry("races", "种族设定"),
            Map.entry("religion", "信仰神明"),
            Map.entry("gods", "信仰神明"),
            Map.entry("uniqueRules", "核心规则"),
            Map.entry("core", "核心规则"),
            Map.entry("ecology", "生态环境"),
            Map.entry("economy", "经济商业"),
            Map.entry("other", "其他")
    );

    /**
     * 分类归一化：英文 ID 转为中文显示名，中文原样返回，空值返回默认
     */
    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "地理版图";
        }
        String trimmed = category.trim();
        return CATEGORY_EN_TO_CN.getOrDefault(trimmed, trimmed);
    }

    /**
     * 查询项目的所有世界观设定
     */
    public List<NovelWorldSetting> listByProjectId(Long projectId) {
        return settingMapper.selectByProjectId(projectId);
    }

    /**
     * 创建世界观设定
     */
    @Transactional
    public NovelWorldSetting create(Long projectId, NovelWorldSetting setting) {
        setting.setId(null);
        setting.setProjectId(projectId);
        setting.setCategory(normalizeCategory(setting.getCategory()));
        if (setting.getStatus() == null) {
            setting.setStatus("draft");
        }
        if (setting.getLevel() == null) {
            setting.setLevel(1);
        }
        if (setting.getRelatedSettings() == null) {
            setting.setRelatedSettings(List.of());
        }
        settingMapper.insert(setting);
        return settingMapper.selectById(setting.getId());
    }

    /**
     * 更新世界观设定（部分更新）
     */
    @Transactional
    public NovelWorldSetting update(Long id, NovelWorldSetting setting) {
        NovelWorldSetting exist = settingMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "设定不存在");
        }
        setting.setId(id);
        // 分类统一转中文（仅在传入时转换）
        if (setting.getCategory() != null) {
            setting.setCategory(normalizeCategory(setting.getCategory()));
        }
        // 固定字段不允许通过此接口修改
        setting.setProjectId(null);
        setting.setCreateTime(null);
        settingMapper.updateById(setting);
        return settingMapper.selectById(id);
    }

    /**
     * 删除世界观设定（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        NovelWorldSetting exist = settingMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(404, "设定不存在");
        }
        settingMapper.deleteById(id);
    }
}
