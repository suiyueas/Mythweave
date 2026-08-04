package com.mythweave.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mythweave.web.common.R;
import com.mythweave.web.dto.ContentGenerateRequest;
import com.mythweave.web.dto.StreamChatRequest;
import com.mythweave.web.dto.StreamWriteRequest;
import com.mythweave.web.entity.NovelAiSession;
import com.mythweave.web.mapper.NovelAiSessionMapper;
import com.mythweave.web.service.AiChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "AI写作助手")
@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final NovelAiSessionMapper sessionMapper;
    private final ObjectMapper objectMapper;

    private static final long HEARTBEAT_INTERVAL_SECONDS = 15;
    private static final long SSE_TIMEOUT_SECONDS = 180;

    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    private final Map<SseEmitter, Long> activeEmitters = new ConcurrentHashMap<>();

    private void startHeartbeat(SseEmitter emitter) {
        long emitterId = System.currentTimeMillis();
        activeEmitters.put(emitter, emitterId);

        ScheduledFuture<?> heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (activeEmitters.get(emitter) == null || activeEmitters.get(emitter) != emitterId) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (IOException e) {
                log.debug("Heartbeat failed, removing emitter");
                activeEmitters.remove(emitter);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        emitter.onCompletion(() -> {
            activeEmitters.remove(emitter);
            heartbeatTask.cancel(true);
        });
        emitter.onTimeout(() -> {
            activeEmitters.remove(emitter);
            heartbeatTask.cancel(true);
        });
        emitter.onError(e -> {
            activeEmitters.remove(emitter);
            heartbeatTask.cancel(true);
        });
    }

    private void stopHeartbeat(SseEmitter emitter) {
        activeEmitters.remove(emitter);
    }

    /**
     * SSE流式续写
     */
    @Operation(summary = "AI流式续写")
    @PostMapping(value = "/stream/write", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamWrite(@PathVariable Long projectId,
                                   @RequestBody StreamWriteRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_SECONDS * 1000L);
        startHeartbeat(emitter);
        CompletableFuture.runAsync(() -> {
            try {
                aiChatService.streamContinueWriting(projectId,
                        request.getContext() != null ? request.getContext() : "",
                        request.getExistingText() != null ? request.getExistingText() : "",
                        request.getTemperature() != null ? request.getTemperature() : 0.7,
                        request.getMaxTokens() != null ? request.getMaxTokens() : 8192,
                        token -> {
                            try {
                                String jsonToken = objectMapper.writeValueAsString(token);
                                emitter.send(SseEmitter.event().data(jsonToken));
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                            }
                        });
                emitter.complete();
            } catch (Exception e) {
                log.error("AI续写异常", e);
                emitter.completeWithError(e);
            } finally {
                stopHeartbeat(emitter);
            }
        });
        return emitter;
    }

    /**
     * SSE流式对话
     */
    @Operation(summary = "AI流式对话")
    @PostMapping(value = "/stream/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable Long projectId,
                                  @RequestBody StreamChatRequest request) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_SECONDS * 1000L);
        startHeartbeat(emitter);
        CompletableFuture.runAsync(() -> {
            try {
                String userMessage = request.getUserMessage() != null ? request.getUserMessage() : "";
                String agent = request.getAgent() != null ? request.getAgent() : "";
                Long sessionId = request.getSessionId();

                log.info("AI流式对话开始, projectId={}, agent={}, sessionId={}, userMessage前50字={}",
                        projectId, agent, sessionId,
                        userMessage.length() > 50 ? userMessage.substring(0, 50) : userMessage);

                saveUserMessage(projectId, sessionId, agent, userMessage);

                StringBuilder fullReply = new StringBuilder();
                aiChatService.streamChat(projectId,
                        request.getNovelTitle() != null ? request.getNovelTitle() : "",
                        request.getGenre() != null ? request.getGenre() : "",
                        request.getCurrentChapter() != null ? request.getCurrentChapter() : "",
                        request.getContext() != null ? request.getContext() : "",
                        userMessage,
                        request.getTemperature() != null ? request.getTemperature() : 0.7,
                        request.getMaxTokens() != null ? request.getMaxTokens() : 8192,
                        token -> {
                            try {
                                fullReply.append(token);
                                String jsonToken = objectMapper.writeValueAsString(token);
                                emitter.send(SseEmitter.event().data(jsonToken).name("msg"));
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                            }
                        },
                        request.getModel());
                saveAssistantReply(projectId, sessionId, agent, fullReply.toString());
                emitter.complete();
                log.info("AI流式对话完成, projectId={}, 回复长度={}", projectId, fullReply.length());
            } catch (Exception e) {
                log.error("AI对话异常: {}", e.toString(), e);
                try {
                    emitter.send(SseEmitter.event().data("❌ AI服务异常：" + e.getMessage()).name("err"));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                stopHeartbeat(emitter);
            }
        });
        return emitter;
    }

    /**
     * 非流式对话
     */
    @Operation(summary = "AI非流式对话")
    @PostMapping("/chat")
    public R<String> chat(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        try {
            String reply = aiChatService.chat(projectId, body.getOrDefault("message", ""));
            return R.ok(reply);
        } catch (Exception e) {
            log.error("AI对话异常", e);
            return R.fail("AI服务异常: " + e.getMessage());
        }
    }

    /**
     * AI 生成章节标题
     */
    @Operation(summary = "AI生成章节标题")
    @PostMapping("/generate-title")
    public R<String> generateTitle(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        try {
            String title = aiChatService.generateTitle(projectId, body);
            if (title == null || title.trim().isEmpty()) {
                log.warn("AI标题生成返回空，使用默认标题");
                return R.ok("第" + body.get("chapterIndex") + "章");
            }
            return R.ok(title.trim());
        } catch (Exception e) {
            log.error("AI标题生成异常", e);
            return R.fail("AI服务异常: " + e.getMessage());
        }
    }

    /**
     * SSE流式生成章节内容
     */
    @Operation(summary = "AI流式生成章节内容")
    @PostMapping(value = "/stream/content", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamContent(@PathVariable Long projectId,
                                     @Valid @RequestBody ContentGenerateRequest request) {
        // 心跳保活：推理型模型在正式输出正文前可能长时间无 content，
        // 前端 20s 无数据会误判断连并自动重连，导致生成中断
        SseEmitter emitter = new SseEmitter(600_000L);
        startHeartbeat(emitter);
        CompletableFuture.runAsync(() -> {
            try {
                aiChatService.streamGenerateContent(projectId, request.getChapterIndex(), request.getTitle(),
                        request.getDirection(), request.getExistingContent(), request.getStyle(),
                        request.getTargetWords(), token -> {
                            try {
                                String jsonToken = objectMapper.writeValueAsString(token);
                                emitter.send(SseEmitter.event().data(jsonToken));
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                            }
                        });
                emitter.complete();
            } catch (Exception e) {
                log.error("AI内容生成异常", e);
                emitter.completeWithError(e);
            } finally {
                stopHeartbeat(emitter);
            }
        });
        return emitter;
    }

    /**
     * AI 润色
     */
    @Operation(summary = "AI润色")
    @PostMapping("/polish")
    public R<String> polish(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        try {
            String text = body.getOrDefault("text", "");
            String style = body.getOrDefault("style", "自然流畅");
            String targetLength = body.getOrDefault("targetLength", "保持原长度");
            String result = aiChatService.polish(projectId, text, style, targetLength);
            return R.ok(result);
        } catch (Exception e) {
            log.error("AI润色异常", e);
            return R.fail("AI服务异常: " + e.getMessage());
        }
    }

    /**
     * AI 扩写
     */
    @Operation(summary = "AI扩写")
    @PostMapping("/expand")
    public R<String> expand(@PathVariable Long projectId, @RequestBody Map<String, Object> body) {
        try {
            String currentContent = body.getOrDefault("currentContent", "") instanceof String
                    ? (String) body.get("currentContent") : "";
            String direction = body.getOrDefault("direction", "延续故事主线，丰富细节") instanceof String
                    ? (String) body.get("direction") : "延续故事主线，丰富细节";
            String style = body.getOrDefault("style", "自然流畅") instanceof String
                    ? (String) body.get("style") : "自然流畅";
            Integer chapterIndex = null;
            if (body.get("chapterIndex") != null) {
                chapterIndex = ((Number) body.get("chapterIndex")).intValue();
            }
            String result = aiChatService.expand(projectId, currentContent, direction, style, chapterIndex);
            return R.ok(result);
        } catch (Exception e) {
            log.error("AI扩写异常", e);
            return R.fail("AI服务异常: " + e.getMessage());
        }
    }

    /**
     * AI 协同创作：带上下文生成章节（非流式）
     */
    @Operation(summary = "AI协同创作生成章节")
    @PostMapping("/generate-chapter")
    public R<Map<String, Object>> generateChapter(@PathVariable Long projectId,
                                                    @RequestBody Map<String, Object> params) {
        try {
            String content = aiChatService.generateChapter(projectId, params);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", content);
            return R.ok(result);
        } catch (Exception e) {
            log.error("AI协同创作异常", e);
            return R.fail("AI服务异常: " + e.getMessage());
        }
    }

    /**
     * SSE 流式协同创作生成章节（带章节衔接上下文）
     */
    @Operation(summary = "AI流式协同创作生成章节")
    @PostMapping(value = "/stream/generate-chapter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerateChapter(@PathVariable Long projectId,
                                             @RequestBody Map<String, Object> params) {
        SseEmitter emitter = new SseEmitter(600_000L);
        startHeartbeat(emitter);
        CompletableFuture.runAsync(() -> {
            try {
                String title = params.get("title") != null ? params.get("title").toString() : "未命名章节";
                String direction = params.get("direction") != null ? params.get("direction").toString() : "延续故事主线";
                String existingContent = params.get("existingContent") != null ? params.get("existingContent").toString() : "";
                String style = params.get("style") != null ? params.get("style").toString() : "自然流畅";
                Integer chapterIndex = params.get("chapterIndex") != null
                        ? ((Number) params.get("chapterIndex")).intValue() : 1;
                Integer targetWords = params.get("targetWords") != null
                        ? ((Number) params.get("targetWords")).intValue() : 2000;

                StringBuilder fullContent = new StringBuilder();
                aiChatService.streamGenerateContent(projectId, chapterIndex, title,
                        direction, existingContent, style, targetWords, token -> {
                            try {
                                fullContent.append(token);
                                String jsonToken = objectMapper.writeValueAsString(token);
                                emitter.send(SseEmitter.event().data(jsonToken));
                            } catch (IOException e) {
                                log.error("SSE send error", e);
                            }
                        });
                emitter.complete();
                log.info("AI流式章节生成完成, projectId={}, 长度={}", projectId, fullContent.length());
            } catch (Exception e) {
                log.error("AI流式章节生成异常", e);
                try {
                    emitter.send(SseEmitter.event().data("❌ AI服务异常：" + e.getMessage()));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                stopHeartbeat(emitter);
            }
        });
        return emitter;
    }

    /**
     * 保存用户的单条消息到会话中（streamChat 启动前调用）
     */
    private void saveUserMessage(Long projectId, Long sessionId, String agent, String userMessage) {
        if (sessionId == null) return;
        try {
            NovelAiSession userSession = new NovelAiSession();
            userSession.setProjectId(projectId);
            userSession.setSessionId(sessionId);
            userSession.setSessionType("chat");
            userSession.setRole("user");
            userSession.setAgent(agent);
            userSession.setContent(userMessage);
            userSession.setTokensUsed(0);
            sessionMapper.insert(userSession);
        } catch (Exception e) {
            log.warn("保存用户消息失败: {}", e.getMessage());
        }
    }

    /**
     * 保存 AI 助手回复到会话中（streamChat 完成后调用）
     */
    private void saveAssistantReply(Long projectId, Long sessionId, String agent, String aiReply) {
        if (sessionId == null) return;
        try {
            NovelAiSession aiSession = new NovelAiSession();
            aiSession.setProjectId(projectId);
            aiSession.setSessionId(sessionId);
            aiSession.setSessionType("chat");
            aiSession.setRole("assistant");
            aiSession.setAgent(agent);
            aiSession.setContent(aiReply);
            aiSession.setTokensUsed(0);
            sessionMapper.insert(aiSession);
        } catch (Exception e) {
            log.warn("保存助手回复失败: {}", e.getMessage());
        }
    }

    // ─── Session 管理 ───

    @Operation(summary = "获取会话列表")
    @GetMapping("/sessions")
    public R<List<Map<String, Object>>> listSessions(@PathVariable Long projectId) {
        try {
            List<NovelAiSession> sessions = sessionMapper.selectDistinctSessions(projectId);
            List<Map<String, Object>> result = sessions.stream().map(s -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", s.getSessionId());
                map.put("createTime", s.getCreateTime());
                // 获取该会话的所有消息
                List<NovelAiSession> msgs = sessionMapper.selectBySessionId(s.getSessionId());
                // 取第一条用户消息作为标题
                String title = msgs.stream()
                        .filter(m -> "user".equals(m.getRole()))
                        .map(NovelAiSession::getContent)
                        .findFirst().orElse("新对话");
                map.put("title", title.length() > 30 ? title.substring(0, 30) + "..." : title);
                // 取最后一条非 system 消息作为预览
                String preview = "";
                for (int i = msgs.size() - 1; i >= 0; i--) {
                    String role = msgs.get(i).getRole();
                    if ("user".equals(role) || "assistant".equals(role)) {
                        preview = msgs.get(i).getContent();
                        break;
                    }
                }
                map.put("preview", preview.length() > 50 ? preview.substring(0, 50) + "..." : preview);
                map.put("messageCount", msgs.size());
                // 使用最后一条消息的时间作为 updateTime，用于排序
                LocalDateTime updateTime = msgs.isEmpty()
                        ? s.getCreateTime()
                        : msgs.get(msgs.size() - 1).getCreateTime();
                map.put("updateTime", updateTime);
                return map;
            }).collect(Collectors.toList());
            // 按 updateTime 降序排列（最新的在最前面）
            result.sort((a, b) -> {
                LocalDateTime ta = (LocalDateTime) a.get("updateTime");
                LocalDateTime tb = (LocalDateTime) b.get("updateTime");
                if (ta == null) return 1;
                if (tb == null) return -1;
                return tb.compareTo(ta);
            });
            return R.ok(result);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return R.fail("获取会话列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建新会话")
    @PostMapping("/sessions")
    public R<Map<String, Object>> createSession(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        try {
            NovelAiSession session = new NovelAiSession();
            session.setProjectId(projectId);
            session.setSessionType("chat");
            session.setRole("system");
            session.setContent(body.getOrDefault("title", "新对话"));
            sessionMapper.insert(session);
            // 关键修复：将 sessionId 设为自增 ID，使同会话的消息能正确分组
            session.setSessionId(session.getId());
            sessionMapper.updateById(session);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", session.getId());
            result.put("sessionId", session.getSessionId());
            result.put("title", body.getOrDefault("title", "新对话"));
            result.put("createTime", session.getCreateTime());
            result.put("updateTime", session.getUpdateTime());
            return R.ok(result);
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return R.fail("创建会话失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取会话消息")
    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<NovelAiSession>> getSessionMessages(@PathVariable Long projectId, @PathVariable Long sessionId) {
        try {
            List<NovelAiSession> messages = sessionMapper.selectBySessionId(sessionId);
            return R.ok(messages);
        } catch (Exception e) {
            log.error("获取会话消息失败", e);
            return R.fail("获取会话消息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/sessions/{sessionId}")
    public R<Void> deleteSession(@PathVariable Long projectId, @PathVariable Long sessionId) {
        try {
            List<NovelAiSession> messages = sessionMapper.selectBySessionId(sessionId);
            for (NovelAiSession msg : messages) {
                sessionMapper.deleteById(msg.getId());
            }
            return R.ok();
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return R.fail("删除会话失败: " + e.getMessage());
        }
    }
}