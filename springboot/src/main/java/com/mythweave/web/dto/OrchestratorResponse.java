package com.mythweave.web.dto;

import com.mythweave.web.model.AgentResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 协调者响应结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrchestratorResponse {
    private AgentResult editorResult;
    private AgentResult characterResult;
    private AgentResult styleResult;
    private AgentResult readerResult;
    private String summary;
    private long totalCostMs;
    private boolean success;
    private String errorMessage;
}