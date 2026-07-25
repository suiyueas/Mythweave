package com.novelcraft.web.service;

import com.novelcraft.web.common.BusinessException;
import com.novelcraft.web.entity.NovelWorldSetting;
import com.novelcraft.web.mapper.NovelWorldSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldSettingService {

    private final NovelWorldSettingMapper settingMapper;

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
        if (setting.getCategory() == null) {
            setting.setCategory("geography");
        }
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
