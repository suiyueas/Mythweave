package com.novelcraft.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcraft.web.entity.NovelSentinelAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<Long, Set<WebSocketSession>> projectSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long projectId = extractProjectId(session);
        if (projectId == null) {
            log.warn("WebSocket 连接缺少 projectId 参数");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        projectSessions.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WebSocket 连接已建立: projectId={}, sessionId={}, 当前连接数={}",
                projectId, session.getId(), projectSessions.get(projectId).size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long projectId = extractProjectId(session);
        if (projectId != null && projectSessions.containsKey(projectId)) {
            projectSessions.get(projectId).remove(session);
            log.info("WebSocket 连接已关闭: projectId={}, sessionId={}, 剩余连接数={}",
                    projectId, session.getId(), projectSessions.get(projectId).size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("收到 WebSocket 消息: sessionId={}, payload={}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket 传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        session.close(CloseStatus.SERVER_ERROR);
    }

    /**
     * 推送哨兵告警到指定项目的所有 WebSocket 客户端
     */
    public void pushSentinelAlerts(Long projectId, List<NovelSentinelAlert> alerts) {
        if (projectId == null || alerts == null || alerts.isEmpty()) return;

        Set<WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("项目 {} 无 WebSocket 连接，跳过推送", projectId);
            return;
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("type", "sentinel_alert");
        payload.put("projectId", projectId);
        payload.put("count", alerts.size());
        payload.put("alerts", alerts.stream().map(this::toAlertDTO).collect(Collectors.toList()));
        payload.put("timestamp", System.currentTimeMillis());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("序列化告警消息失败: {}", e.getMessage());
            return;
        }

        int successCount = 0;
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                    successCount++;
                } catch (IOException e) {
                    log.warn("推送消息到 session {} 失败: {}", session.getId(), e.getMessage());
                }
            }
        }
        log.info("✅ 哨兵告警已推送至项目 {}: {} 条告警, {} 个连接", projectId, alerts.size(), successCount);
    }

    /**
     * 推送通用通知到指定项目
     */
    public void pushNotification(Long projectId, String title, String message, String level) {
        if (projectId == null) return;

        Set<WebSocketSession> sessions = projectSessions.get(projectId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("项目 {} 无 WebSocket 连接，跳过推送", projectId);
            return;
        }

        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("type", "notification");
        payload.put("projectId", projectId);
        payload.put("title", title);
        payload.put("message", message);
        payload.put("level", level);
        payload.put("timestamp", System.currentTimeMillis());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("序列化通知消息失败: {}", e.getMessage());
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (IOException e) {
                    log.warn("推送通知到 session {} 失败: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    private Map<String, Object> toAlertDTO(NovelSentinelAlert alert) {
        Map<String, Object> dto = new java.util.LinkedHashMap<>();
        dto.put("id", alert.getId());
        dto.put("type", alert.getType());
        dto.put("title", alert.getTitle());
        dto.put("description", alert.getDescription());
        dto.put("severity", alert.getSeverity());
        dto.put("suggestion", alert.getSuggestion());
        dto.put("chapterId", alert.getChapterId());
        dto.put("resolved", alert.getResolved());
        return dto;
    }

    private Long extractProjectId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && "projectId".equals(kv[0])) {
                try {
                    return Long.parseLong(kv[1]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    public int getConnectionCount(Long projectId) {
        if (projectId == null) return 0;
        Set<WebSocketSession> sessions = projectSessions.get(projectId);
        return sessions != null ? sessions.size() : 0;
    }
}