package com.mythweave.web.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审计日志服务
 * 
 * 记录敏感操作的完整审计日志，包括：
 * - 用户登录/登出
 * - VIP 购买/激活
 * - 作品删除
 * - 批量操作
 * - 敏感数据访问
 * 
 * 日志格式：时间 | 用户ID | 操作类型 | 目标类型:目标ID | IP | 详情
 */
@Slf4j
@Service
public class AuditLogService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /** 操作类型枚举 */
    public enum Action {
        // 认证相关
        LOGIN("用户登录"),
        LOGOUT("用户登出"),
        LOGIN_FAILED("登录失败"),
        
        // VIP 相关
        VIP_ACTIVATE("VIP激活"),
        VIP_RENEW("VIP续费"),
        VIP_UPGRADE("VIP升级"),
        
        // 作品管理
        PROJECT_CREATE("创建作品"),
        PROJECT_DELETE("删除作品"),
        PROJECT_UPDATE("更新作品"),
        PROJECT_SHARE("分享作品"),
        
        // 章节管理
        CHAPTER_CREATE("创建章节"),
        CHAPTER_DELETE("删除章节"),
        CHAPTER_PUBLISH("发布章节"),
        CHAPTER_REVERT("回滚章节"),
        
        // 管理员操作
        ADMIN_USER_DELETE("管理员删除用户"),
        ADMIN_PROJECT_DELETE("管理员删除作品"),
        ADMIN_DATA_EXPORT("管理员数据导出"),
        
        // 支付相关
        PAYMENT_INIT("支付初始化"),
        PAYMENT_COMPLETE("支付完成"),
        PAYMENT_FAILED("支付失败"),
        
        // AI 服务
        AI_CONTENT_GENERATE("AI内容生成"),
        AI_OUTLINE_GENERATE("AI大纲生成"),
        AI_CHARACTER_GENERATE("AI角色生成"),
        
        // 数据安全
        PASSWORD_CHANGE("修改密码"),
        PASSWORD_RESET("重置密码"),
        EMAIL_CHANGE("修改邮箱"),
        API_KEY_ACCESS("API密钥访问");
        
        private final String description;
        
        Action(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /** 审计日志构建器 */
    public static class AuditLogBuilder {
        private final AuditLogService service;
        private Long userId;
        private Action action;
        private String targetType;
        private Long targetId;
        private String ipAddress;
        private String userAgent;
        private Map<String, Object> details;
        private boolean success = true;
        private String errorMessage;
        
        AuditLogBuilder(AuditLogService service) {
            this.service = service;
        }
        
        public AuditLogBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }
        
        public AuditLogBuilder action(Action action) {
            this.action = action;
            return this;
        }
        
        public AuditLogBuilder target(String targetType, Long targetId) {
            this.targetType = targetType;
            this.targetId = targetId;
            return this;
        }
        
        public AuditLogBuilder ip(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public AuditLogBuilder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public AuditLogBuilder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }
        
        public AuditLogBuilder details(String key, Object value) {
            if (this.details == null) {
                this.details = new ConcurrentHashMap<>();
            }
            this.details.put(key, value);
            return this;
        }
        
        public AuditLogBuilder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public AuditLogBuilder error(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }
        
        public void log() {
            service.log(this);
        }
    }
    
    /**
     * 创建审计日志构建器
     */
    public AuditLogBuilder builder() {
        return new AuditLogBuilder(this);
    }
    
    /**
     * 记录审计日志
     */
    private void log(AuditLogBuilder builder) {
        StringBuilder sb = new StringBuilder();
        sb.append("AUDIT | ");
        sb.append(LocalDateTime.now().format(FORMATTER)).append(" | ");
        sb.append("userId=").append(builder.userId).append(" | ");
        sb.append("action=").append(builder.action != null ? builder.action.getDescription() : "UNKNOWN").append(" | ");
        
        if (builder.targetType != null) {
            sb.append("target=").append(builder.targetType);
            if (builder.targetId != null) {
                sb.append(":").append(builder.targetId);
            }
            sb.append(" | ");
        }
        
        if (builder.ipAddress != null) {
            sb.append("ip=").append(builder.ipAddress).append(" | ");
        }
        
        sb.append("success=").append(builder.success).append(" | ");
        
        if (builder.details != null && !builder.details.isEmpty()) {
            sb.append("details=").append(builder.details).append(" | ");
        }
        
        if (!builder.success && builder.errorMessage != null) {
            sb.append("error=").append(builder.errorMessage);
        }
        
        if (builder.success) {
            log.info(sb.toString());
        } else {
            log.warn(sb.toString());
        }
    }
    
    // ==================== 快捷记录方法 ====================
    
    public void logVipActivation(Long userId, String planId, String ip, boolean success) {
        builder()
            .userId(userId)
            .action(Action.VIP_ACTIVATE)
            .details("planId", planId)
            .ip(ip)
            .success(success)
            .log();
    }
    
    public void logProjectDelete(Long userId, Long projectId, String ip, boolean success) {
        builder()
            .userId(userId)
            .action(Action.PROJECT_DELETE)
            .target("project", projectId)
            .ip(ip)
            .success(success)
            .log();
    }
    
    public void logChapterDelete(Long userId, Long chapterId, Long projectId, String ip, boolean success) {
        builder()
            .userId(userId)
            .action(Action.CHAPTER_DELETE)
            .target("chapter", chapterId)
            .details("projectId", projectId)
            .ip(ip)
            .success(success)
            .log();
    }
    
    public void logLogin(Long userId, String ip, String userAgent, boolean success) {
        builder()
            .userId(userId)
            .action(success ? Action.LOGIN : Action.LOGIN_FAILED)
            .ip(ip)
            .userAgent(userAgent)
            .success(success)
            .log();
    }
    
    public void logPasswordChange(Long userId, String ip, boolean success) {
        builder()
            .userId(userId)
            .action(Action.PASSWORD_CHANGE)
            .ip(ip)
            .success(success)
            .log();
    }
    
    public void logAiGenerate(Long userId, Long projectId, String actionType, String ip) {
        builder()
            .userId(userId)
            .action(Action.AI_CONTENT_GENERATE)
            .target("project", projectId)
            .details("actionType", actionType)
            .ip(ip)
            .success(true)
            .log();
    }
}