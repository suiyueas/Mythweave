package com.novelcraft.web.model;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多Agent协作的共享上下文
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

    private Map<String, String> agentResults = new ConcurrentHashMap<>();

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