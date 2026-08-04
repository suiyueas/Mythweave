package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.entity.NovelUserSettings;
import com.mythweave.web.mapper.NovelUserSettingsMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Slf4j
@Tag(name = "个人设置")
@RestController
@RequestMapping("/api/users/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final NovelUserSettingsMapper settingsMapper;
    private final ObjectMapper objectMapper;
    
    private static final Long DEFAULT_USER_ID = 1L;

    @Operation(summary = "获取用户设置")
    @GetMapping
    public R<Map<String, Object>> getSettings() {
        try {
            Map<String, Object> result = new LinkedHashMap<>();
            
            // 编辑器设置
            Map<String, Object> editor = getSettingsByCategory("editor");
            result.put("editor", editor);
            
            // 外观设置
            Map<String, Object> appearance = getSettingsByCategory("appearance");
            result.put("appearance", appearance);
            
            // 通知设置
            Map<String, Object> notification = getSettingsByCategory("notification");
            result.put("notification", notification);
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取设置失败", e);
            return R.fail("获取设置失败");
        }
    }

    @Operation(summary = "更新用户设置")
    @PutMapping
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> updateSettings(@RequestBody Map<String, Object> settings) {
        try {
            // 更新编辑器设置
            if (settings.containsKey("editor") && settings.get("editor") instanceof Map) {
                saveSettingsByCategory("editor", (Map<String, Object>) settings.get("editor"));
            }
            
            // 更新外观设置
            if (settings.containsKey("appearance") && settings.get("appearance") instanceof Map) {
                saveSettingsByCategory("appearance", (Map<String, Object>) settings.get("appearance"));
            }
            
            // 更新通知设置
            if (settings.containsKey("notification") && settings.get("notification") instanceof Map) {
                saveSettingsByCategory("notification", (Map<String, Object>) settings.get("notification"));
            }
            
            return R.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("保存设置失败", e);
            return R.fail("保存设置失败");
        }
    }

    private Map<String, Object> getSettingsByCategory(String category) {
        Map<String, Object> result = new HashMap<>();
        
        List<NovelUserSettings> settingsList = settingsMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelUserSettings>()
                .eq("user_id", DEFAULT_USER_ID)
                .likeRight("setting_key", category + ".")
        );
        
        for (NovelUserSettings setting : settingsList) {
            String key = setting.getSettingKey().replace(category + ".", "");
            Object value = parseValue(setting.getSettingValue());
            result.put(key, value);
        }
        
        // 返回默认值如果为空
        if (result.isEmpty()) {
            return getDefaultSettings(category);
        }
        
        return result;
    }

    private Map<String, Object> getDefaultSettings(String category) {
        Map<String, Object> defaults = new HashMap<>();
        
        switch (category) {
            case "editor":
                defaults.put("theme", "warm-ivory");
                defaults.put("fontSize", 16);
                defaults.put("lineHeight", 1.65);
                defaults.put("fontFamily", "Crimson Pro");
                defaults.put("writingMode", "dual");
                defaults.put("autoSave", true);
                defaults.put("saveInterval", 5);
                defaults.put("cloudSync", true);
                defaults.put("versionHistory", 20);
                break;
            case "appearance":
                defaults.put("themeColor", "amber");
                defaults.put("sidebarPosition", "left");
                defaults.put("sidebarWidth", "standard");
                defaults.put("showSidebar", true);
                defaults.put("compactMode", false);
                defaults.put("pageTransition", true);
                defaults.put("hoverEffect", true);
                defaults.put("loadingAnimation", true);
                defaults.put("backgroundTexture", true);
                break;
            case "notification":
                defaults.put("sentinelAlert", true);
                defaults.put("agentComplete", true);
                defaults.put("writingGoal", true);
                defaults.put("versionSave", false);
                defaults.put("notificationBell", true);
                defaults.put("soundAlert", false);
                defaults.put("severityThreshold", "warning");
                defaults.put("quietHours", Map.of("enabled", false, "start", "22:00", "end", "08:00"));
                break;
        }
        
        return defaults;
    }

    private void saveSettingsByCategory(String category, Map<String, Object> settings) throws JsonProcessingException {
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            String key = category + "." + entry.getKey();
            String value;
            
            if (entry.getValue() instanceof Map) {
                value = objectMapper.writeValueAsString(entry.getValue());
            } else {
                value = String.valueOf(entry.getValue());
            }
            
            // 检查是否已存在
            NovelUserSettings existing = settingsMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<NovelUserSettings>()
                    .eq("user_id", DEFAULT_USER_ID)
                    .eq("setting_key", key)
            );
            
            if (existing != null) {
                existing.setSettingValue(value);
                settingsMapper.updateById(existing);
            } else {
                NovelUserSettings newSetting = new NovelUserSettings();
                newSetting.setUserId(DEFAULT_USER_ID);
                newSetting.setSettingKey(key);
                newSetting.setSettingValue(value);
                settingsMapper.insert(newSetting);
            }
        }
    }

    private Object parseValue(String value) {
        if (value == null) return null;
        
        // 尝试解析JSON
        if (value.startsWith("{") || value.startsWith("[")) {
            try {
                return objectMapper.readValue(value, Object.class);
            } catch (JsonProcessingException e) {
                // 如果解析失败，作为字符串返回
            }
        }
        
        // 布尔值
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        
        // 数字
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 字符串
        }
        
        return value;
    }
}