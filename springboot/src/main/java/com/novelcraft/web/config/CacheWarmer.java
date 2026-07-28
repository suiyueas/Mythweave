package com.novelcraft.web.config;

import com.novelcraft.web.mapper.NovelProjectMapper;
import com.novelcraft.web.service.DashboardCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 缓存预热：项目启动时加载最近活跃项目的仪表盘缓存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmer implements ApplicationRunner {

    private final NovelProjectMapper projectMapper;
    private final DashboardCacheService dashboardCacheService;

    @Override
    public void run(ApplicationArguments args) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Long> activeProjectIds = projectMapper.selectActiveProjectIdsWithinDays(7);
                if (activeProjectIds == null || activeProjectIds.isEmpty()) {
                    log.info("缓存预热：无最近活跃项目");
                    return;
                }
                log.info("缓存预热开始，共 {} 个项目", activeProjectIds.size());
                for (Long projectId : activeProjectIds) {
                    dashboardCacheService.getStats(projectId);
                }
                log.info("缓存预热完成");
            } catch (Exception e) {
                log.warn("缓存预热异常: {}", e.getMessage());
            }
        });
    }
}
