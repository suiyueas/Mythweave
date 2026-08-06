package com.mythweave.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

public class StreamWriteRequest {
    private String context;
    private String existingText;

    @Min(value = 0, message = "temperature必须在0-2之间")
    @Max(value = 2, message = "temperature必须在0-2之间")
    private Double temperature = 0.7;

    @Positive(message = "maxTokens必须为正数")
    @Max(value = 32768, message = "maxTokens不能超过32768")
    private Integer maxTokens = 8192;

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getExistingText() { return existingText; }
    public void setExistingText(String existingText) { this.existingText = existingText; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
}