package com.novelcraft.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;

/**
 * NovelCraft AI小说创作平台 - 应用启动入口
 * 
 * 功能概述：
 * - AI辅助小说创作管理平台的后端服务
 * - 提供作品管理、章节管理、角色管理、世界观设定等核心功能
 * - 支持AI写作、灵感生成、哨兵扫描等高级功能
 */
@SpringBootApplication(scanBasePackages = "com.novelcraft.web")
public class NovelCraftApplication {
    
    /**
     * 应用主入口，启动Spring Boot应用
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NovelCraftApplication.class, args);
    }

    /**
     * 应用启动完成后的回调事件处理
     * 用于输出启动成功提示信息，便于运维人员确认服务状态
     */
    @EventListener(ApplicationReadyEvent.class)
    public void handleApplicationReadyEvent() {
        System.out.println("=================================="); 
        System.out.println("后端启动成功！Welcome to NovelCraft!");
        System.out.println("=================================="); 
    }
}