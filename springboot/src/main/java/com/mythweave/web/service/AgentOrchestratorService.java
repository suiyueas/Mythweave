package com.mythweave.web.service;

import com.mythweave.web.dto.OrchestratorRequest;
import com.mythweave.web.dto.OrchestratorResponse;
import com.mythweave.web.service.agent.OrchestratorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final OrchestratorAgent orchestratorAgent;

    public OrchestratorResponse orchestrate(Long projectId, OrchestratorRequest request) {
        return orchestratorAgent.orchestrate(projectId, request);
    }
}