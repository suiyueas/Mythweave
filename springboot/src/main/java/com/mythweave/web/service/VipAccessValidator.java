package com.mythweave.web.service;

import com.mythweave.web.common.BusinessException;
import com.mythweave.web.entity.NovelUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * VIP 权限验证器
 * 
 * 提供后端二次 VIP 校验，防止前端绕过权限限制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipAccessValidator {

    private final UserService userService;

    /** VIP 角色标识 */
    private static final String ROLE_ADMIN = "admin";
    
    /** 最低 VIP 等级要求 */
    private static final int MIN_VIP_LEVEL = 1;

    /**
     * 校验用户是否具有 VIP 权限
     * 
     * @param userId 用户ID（从请求上下文中获取）
     * @throws BusinessException 如果用户无 VIP 权限
     */
    public void validateVipAccess(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        NovelUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 管理员不受 VIP 限制
        if (ROLE_ADMIN.equals(user.getRole())) {
            return;
        }
        
        String vipStatus = calcVipStatus(user);
        if (!"active".equals(vipStatus)) {
            log.warn("VIP 权限校验失败: userId={}, vipLevel={}, status={}", 
                    userId, user.getVipLevel(), vipStatus);
            throw new BusinessException(403, "VIP权限不足，请先开通会员");
        }
    }
    
    /**
     * 校验用户是否具有特定 VIP 等级
     * 
     * @param userId 用户ID
     * @param requiredLevel 需要的 VIP 等级
     * @throws BusinessException 如果用户 VIP 等级不足
     */
    public void validateVipLevel(Long userId, int requiredLevel) {
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        
        NovelUser user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 管理员不受 VIP 等级限制
        if (ROLE_ADMIN.equals(user.getRole())) {
            return;
        }
        
        Integer vipLevel = user.getVipLevel();
        if (vipLevel == null || vipLevel < requiredLevel) {
            log.warn("VIP 等级校验失败: userId={}, currentLevel={}, requiredLevel={}", 
                    userId, vipLevel, requiredLevel);
            throw new BusinessException(403, "VIP等级不足，需要等级 " + requiredLevel + " 才能使用该功能");
        }
        
        String vipStatus = calcVipStatus(user);
        if (!"active".equals(vipStatus)) {
            throw new BusinessException(403, "VIP会员已过期，请续费后使用");
        }
    }
    
    /**
     * 获取用户 VIP 状态
     * 
     * @param userId 用户ID
     * @return VIP 状态：active-生效中 / expired-已过期 / none-未开通
     */
    public String getVipStatus(Long userId) {
        if (userId == null) {
            return "none";
        }
        NovelUser user = userService.getById(userId);
        if (user == null) {
            return "none";
        }
        return calcVipStatus(user);
    }
    
    /**
     * 计算 VIP 状态
     */
    private String calcVipStatus(NovelUser user) {
        Integer level = user.getVipLevel();
        if (level == null || level <= 0) {
            return "none";
        }
        if (user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(java.time.LocalDateTime.now())) {
            return "active";
        }
        return "expired";
    }
}