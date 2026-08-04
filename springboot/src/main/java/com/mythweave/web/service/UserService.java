package com.mythweave.web.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mythweave.web.entity.NovelUser;
import com.mythweave.web.entity.NovelUserStats;
import com.mythweave.web.mapper.NovelUserMapper;
import com.mythweave.web.mapper.NovelUserStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<NovelUserMapper, NovelUser> {

    private final NovelUserStatsMapper userStatsMapper;
    private final AuditLogService auditLogService;

    /** VIP 套餐：planId → 时长（月） */
    private static final Map<String, Integer> VIP_PLAN_MONTHS = Map.of(
            "monthly", 1,
            "quarterly", 3,
            "yearly", 12
    );
    
    /** 管理员角色标识 */
    private static final String ROLE_ADMIN = "admin";
    /** 兼容旧数据：历史默认用户用户名（无 role 字段时按此兜底识别管理员） */
    private static final String LEGACY_ADMIN_USERNAME = "admin";
    /** 管理员 VIP 到期时间（永久有效） */
    private static final LocalDateTime ADMIN_VIP_EXPIRE = LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    
    /**
     * 获取当前登录用户 ID（由 JwtFilter 从 token 解析后写入 request attribute）
     */
    public Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new IllegalStateException("无法获取当前登录用户");
        }
        Object userId = attrs.getRequest().getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("未登录或会话已过期");
        }
        return Long.valueOf(userId.toString());
    }

    /**
     * 获取当前登录用户信息（按 token 动态查询，不再固定返回 ID=1）
     */
    public NovelUser getProfile() {
        NovelUser user = getById(getCurrentUserId());
        ensureAdminVip(user);
        return user;
    }
    
    /**
     * 管理员默认 VIP：role=admin 的用户自动补全为钻石等级（永久有效）。
     * 兼容旧库：username=admin 的历史用户自动升级为 admin 角色并持久化。
     */
    private void ensureAdminVip(NovelUser user) {
        if (user == null) return;
        if (!ROLE_ADMIN.equals(user.getRole())) {
            // 历史兼容：用户名 admin 且角色缺失/非 admin 时，自动补全角色
            if (LEGACY_ADMIN_USERNAME.equals(user.getUsername())) {
                user.setRole(ROLE_ADMIN);
                updateById(user);
                log.info("历史管理员角色自动补全: userId=" + user.getId());
            } else {
                return;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        boolean needVip = user.getVipLevel() == null || user.getVipLevel() <= 0
                || user.getVipExpireAt() == null || !user.getVipExpireAt().isAfter(now);
        if (!needVip) return;
        user.setVipLevel(3);
        user.setVipExpireAt(ADMIN_VIP_EXPIRE);
        if (user.getVipPurchasedAt() == null) {
            user.setVipPurchasedAt(now);
        }
        updateById(user);
        log.info("管理员 VIP 自动补全/续期: userId=" + user.getId());
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    public void updateProfile(NovelUser user) {
        user.setId(getCurrentUserId());
        updateById(user);
    }
    
    /**
     * 获取用户统计
     */
    public NovelUserStats getStats() {
        return userStatsMapper.selectById(getCurrentUserId());
    }
    
    /**
     * 获取用户信息和统计（合并返回）
     */
    public Map<String, Object> getFullProfile() {
        Map<String, Object> result = new HashMap<>();
        
        NovelUser user = getProfile();
        if (user != null) {
            result.put("id", user.getId());
            result.put("nickname", user.getNickname());
            result.put("email", user.getEmail());
            result.put("phone", user.getPhone());
            result.put("bio", user.getBio());
            result.put("avatar", user.getAvatar());
            result.put("createdAt", user.getCreateTime());
            result.put("emailVerified", user.getEmailVerified());
            result.put("role", user.getRole());
            result.put("vipLevel", user.getVipLevel());
            result.put("vipExpireAt", user.getVipExpireAt());
            result.put("vipPurchasedAt", user.getVipPurchasedAt());
            result.put("vipStatus", calcVipStatus(user));
        }
        
        NovelUserStats stats = getStats();
        if (stats != null) {
            result.put("totalWords", stats.getTotalWords());
            result.put("continuousDays", stats.getContinuousDays());
            result.put("worksCount", stats.getWorksCount());
            result.put("level", stats.getUserLevel());
        }
        
        return result;
    }
    
    /** 计算 VIP 状态：active-生效中 / expired-已过期 / none-未开通 */
    private String calcVipStatus(NovelUser user) {
        Integer level = user.getVipLevel();
        if (level == null || level <= 0) return "none";
        if (user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(LocalDateTime.now())) {
            return "active";
        }
        return "expired";
    }
    
    /**
     * 获取 VIP 套餐列表（价格与权益说明，支付接入前为占位配置）
     */
    public List<Map<String, Object>> getVipPlans() {
        List<Map<String, Object>> plans = new ArrayList<>();
        plans.add(vipPlan("monthly", "月度会员", 30, 1, "¥30/月", "适合尝鲜体验"));
        plans.add(vipPlan("quarterly", "季度会员", 78, 3, "¥26/月", "省 ¥12"));
        plans.add(vipPlan("yearly", "年度会员", 298, 12, "¥24.8/月", "省 ¥62，最划算"));
        return plans;
    }
    
    private Map<String, Object> vipPlan(String id, String name, int price, int months, String unitPrice, String badge) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("id", id);
        plan.put("name", name);
        plan.put("price", price);
        plan.put("months", months);
        plan.put("unitPrice", unitPrice);
        plan.put("badge", badge);
        return plan;
    }
    
    /**
     * 激活/续费 VIP（模拟支付成功）：生效中续费则累加时长，否则从当前时间起算
     */
    @Transactional
    public Map<String, Object> activateVip(String planId) {
        Integer months = VIP_PLAN_MONTHS.get(planId);
        if (months == null) {
            throw new IllegalArgumentException("未知的 VIP 套餐: " + planId);
        }
        NovelUser user = getProfile();
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
    
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime base = (user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(now))
                ? user.getVipExpireAt() : now;
        LocalDateTime expireAt = base.plusMonths(months);
    
        user.setVipLevel(ROLE_ADMIN.equals(user.getRole()) ? 3 : 1);
        user.setVipExpireAt(expireAt);
        user.setVipPurchasedAt(now);
        updateById(user);
    
        log.info("VIP 激活成功: userId=" + user.getId() + ", planId=" + planId + ", expireAt=" + expireAt);
        
        // 审计日志
        String ip = getClientIp();
        auditLogService.logVipActivation(user.getId(), planId, ip, true);
    
        Map<String, Object> vip = new LinkedHashMap<>();
        vip.put("vipLevel", user.getVipLevel());
        vip.put("vipExpireAt", expireAt);
        vip.put("vipPurchasedAt", now);
        vip.put("vipStatus", "active");
        return vip;
    }
    
    /**
     * 修改密码
     */
    @Transactional
    public boolean changePassword(String oldPassword, String newPassword) {
        NovelUser user = getById(getCurrentUserId());
        if (user == null) {
            return false;
        }
        // 注册时密码使用 BCrypt 加密，校验需用 matches，不能明文比较
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(encoder.encode(newPassword));
        return updateById(user);
    }
    
    /**
     * 发送邮箱验证（模拟）
     */
    public boolean sendEmailVerification() {
        NovelUser user = getById(getCurrentUserId());
        if (user == null || user.getEmail() == null) {
            return false;
        }
        // 实际项目应发送验证邮件
        return true;
    }
    
    /**
     * 获取客户端 IP 地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            var request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip != null ? ip : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}