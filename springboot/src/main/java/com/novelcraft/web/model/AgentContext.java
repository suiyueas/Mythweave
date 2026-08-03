package com.novelcraft.web.model;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多Agent协作的共享上下文
 * 
 * 用于在OrchestratorAgent协调多个专业Agent时传递统一的信息
 * 包含项目信息、章节内容、角色档案、伏笔信息等
 * 
 * 使用ThreadLocal安全的ConcurrentHashMap存储各Agent的分析结果
 * 允许并行执行时各Agent独立写入结果而不产生竞态条件
 * 
 * 设计模式：Context Object（上下文对象）
 */
@Data
public class AgentContext {
    
    /** 作品ID */
    private Long projectId;
    
    /** 作品标题 */
    private String projectTitle;
    
    /** 作品类型/题材 */
    private String genre;

    /** 章节内容（需要分析的文本） */
    private String chapterContent;
    
    /** 章节标题 */
    private String chapterTitle;
    
    /** 章节序号 */
    private Integer chapterIndex;

    /** 角色档案列表 */
    private List<CharacterInfo> characters;
    
    /** 伏笔信息列表 */
    private List<ForeshadowingInfo> foreshadowings;

    /** 黄金样本（参考风格文本） */
    private String goldSamples;
    
    /** 目标读者类型 */
    private String readerType;

    /** 各Agent的分析结果（key: agentKey, value: 分析结果） */
    private Map<String, String> agentResults = new ConcurrentHashMap<>();

    /**
     * 角色信息内部类
     * 
     * 存储单个角色的完整档案信息
     */
    @Data
    public static class CharacterInfo {
        private Long id;
        private String name;
        private String role;
        private String personality;
        private String background;
        private String arc;
    }

    /**
     * 伏笔信息内部类
     * 
     * 存储单个伏笔的完整信息
     */
    @Data
    public static class ForeshadowingInfo {
        private Long id;
        private String name;
        private String description;
        private Integer chapterId;
        private Integer resolvedChapterId;
        private String status;
    }
}