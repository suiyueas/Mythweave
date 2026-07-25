package com.novelcraft.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;


@SpringBootApplication(scanBasePackages = "com.novelcraft.web")
public class NovelCraftApplication {
    public static void main(String[] args) {
        SpringApplication.run(NovelCraftApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void handleApplicationReadyEvent() {
        System.out.println("=================================="); 
        System.out.println("后端启动成功！Welcome to NovelCraft!");
        System.out.println("=================================="); 
    }
}
