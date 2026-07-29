package com.novelcraft.web.service;

import com.novelcraft.web.dto.OrchestratorRequest;
import com.novelcraft.web.dto.OrchestratorResponse;
import com.novelcraft.web.service.agent.OrchestratorAgent;
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