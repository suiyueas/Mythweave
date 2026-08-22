package com.mythweave.web.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多Agent协作的共享上下文
 *
 * 设计要点：
 * - 输入字段在构建后不可变（通过 snapshot() 生成不可变快照）
 * - agentResults 使用 ConcurrentHashMap 支持并行写入，各Agent独立写入结果无竞态
 * - snapshot() 返回防御性副本，列表用 unmodifiableList 包装，防止Agent修改输入数据
 *
 * 设计模式：Context Object（上下文对象）+ 值对象语义
 */
@Data
public class AgentContext {

    private Long projectId;
    private String projectTitle;
    private String genre;
    private String chapterContent;
    private String chapterTitle;
    private Integer chapterIndex;
    private List<CharacterInfo> characters;
    private List<ForeshadowingInfo> foreshadowings;
    private String goldSamples;
    private String readerType;

    /** 各Agent的分析结果（key: agentKey, value: 分析结果），并行安全 */
    private Map<String, String> agentResults = new ConcurrentHashMap<>();

    /**
     * 生成不可变快照：用于并行分发给多个Agent。
     * 输入字段包装为不可变，agentResults 为新的 ConcurrentHashMap 副本，
     * 防止Agent修改输入数据影响其他Agent。
     */
    public AgentContext snapshot() {
        AgentContext copy = new AgentContext();
        copy.projectId = this.projectId;
        copy.projectTitle = this.projectTitle;
        copy.genre = this.genre;
        copy.chapterContent = this.chapterContent;
        copy.chapterTitle = this.chapterTitle;
        copy.chapterIndex = this.chapterIndex;
        copy.goldSamples = this.goldSamples;
        copy.readerType = this.readerType;
        copy.characters = this.characters != null
                ? Collections.unmodifiableList(new ArrayList<>(this.characters))
                : List.of();
        copy.foreshadowings = this.foreshadowings != null
                ? Collections.unmodifiableList(new ArrayList<>(this.foreshadowings))
                : List.of();
        copy.agentResults = new ConcurrentHashMap<>();
        return copy;
    }

    @Data
    public static class CharacterInfo {
        private Long id;
        private String name;
        private String role;
        private String personality;
        private String background;
        private String arc;
    }

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
