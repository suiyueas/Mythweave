package com.novelcraft.web.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.novelcraft.web.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 会话记录（含 Chat 对话与 Agent 任务）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("novel_ai_session")
public class NovelAiSession extends BaseEntity {
    private Long projectId;
    private Long sessionId;
    private String sessionType;
    private String role;
    private String agent;
    private String content;
    private Integer tokensUsed;
}
