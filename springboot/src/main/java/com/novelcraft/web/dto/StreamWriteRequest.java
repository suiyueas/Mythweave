package com.novelcraft.web.dto;

public class StreamWriteRequest {
    private String context;
    private String existingText;
    private Double temperature;
    private Integer maxTokens;

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getExistingText() { return existingText; }
    public void setExistingText(String existingText) { this.existingText = existingText; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}