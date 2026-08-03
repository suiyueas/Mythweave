package com.novelcraft.web.dto;

public class StreamChatRequest {
    private String userMessage;
    private String novelTitle;
    private String genre;
    private String currentChapter;
    private String context;
    private String agent;
    private Long sessionId;
    private Double temperature;
    private Integer maxTokens;
    private String model;

    public String getUserMessage() { return userMessage; }
    public void setUserMessage(String userMessage) { this.userMessage = userMessage; }

    public String getNovelTitle() { return novelTitle; }
    public void setNovelTitle(String novelTitle) { this.novelTitle = novelTitle; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getCurrentChapter() { return currentChapter; }
    public void setCurrentChapter(String currentChapter) { this.currentChapter = currentChapter; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}