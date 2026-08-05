package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.entity.NovelUser;
import com.mythweave.web.entity.NovelUserStats;
import com.mythweave.web.service.AvatarStorageService;
import com.mythweave.web.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户信息管理控制器
 * 
 * 主要功能：
 * - 用户个人资料查看与更新
 * - 用户统计数据查询
 * - 用户头像上传与删除
 * - 修改密码
 * - 邮箱验证
 * 
 * 所有接口都需要用户登录认证
 */
@Slf4j
@Tag(name = "用户管理", description = "用户信息、头像、VIP会员管理")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final AvatarStorageService avatarStorageService;
    
    /**
     * 获取当前登录用户的完整个人信息
     * @return 用户信息（包含基本信息、VIP状态、统计信息等）
     */
    @GetMapping("/profile")
    @Operation(summary = "获取用户个人资料")
    public R<Map<String, Object>> getProfile() {
        return R.ok(userService.getFullProfile());
    }
    
    /**
     * 更新当前登录用户的信息
     * @param user 只更新传入的字段，未传入字段保持不变
     * @return 操作结果
     */
    @PutMapping("/profile")
    @Operation(summary = "更新用户个人资料")
    public R<Void> updateProfile(@RequestBody NovelUser user) {
        userService.updateProfile(user);
        return R.ok();
    }
    
    /**
     * 获取当前登录用户的统计数据
     * @return 用户统计信息（作品数、字数、VIP等级等）
     */
    @GetMapping("/stats")
    @Operation(summary = "获取用户统计数据")
    public R<NovelUserStats> getStats() {
        return R.ok(userService.getStats());
    }
    
    /**
     * 上传用户头像
     * 
     * 处理流程：
     * 1. 验证文件格式和大小
     * 2. 获取当前用户旧头像URL
     * 3. 删除旧头像文件（如果存在）
     * 4. 保存新头像文件
     * 5. 更新数据库中的头像URL
     * 
     * @param file 头像图片文件（支持jpg、png、gif格式）
     * @return 新头像的URL地址
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传用户头像")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 从 token 解析当前用户 ID
            Long userId = userService.getCurrentUserId();
            
            // 获取当前用户信息
            Map<String, Object> profile = userService.getFullProfile();
            String oldAvatar = (String) profile.get("avatar");
            
            // 删除旧头像
            if (oldAvatar != null && !oldAvatar.isEmpty()) {
                avatarStorageService.deleteAvatar(oldAvatar);
            }
            
            // 保存新头像
            String avatarUrl = avatarStorageService.saveAvatar(file, userId);
            
            // 更新数据库中的头像URL
            NovelUser user = new NovelUser();
            user.setId(userId);
            user.setAvatar(avatarUrl);
            userService.updateProfile(user);
            
            log.info("头像上传成功: userId={}, avatarUrl={}", userId, avatarUrl);
            return R.ok(Map.of("avatarUrl", avatarUrl));
            
        } catch (IllegalArgumentException e) {
            log.warn("头像上传失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        } catch (Exception e) {
            log.error("头像上传异常", e);
            return R.fail("头像上传失败，请重试");
        }
    }
    
    /**
     * 删除用户头像
     * 
     * 处理流程：
     * 1. 获取当前用户头像URL
     * 2. 删除头像文件（如果存在）
     * 3. 清空数据库中的头像URL
     * 
     * @return 操作结果
     */
    @DeleteMapping("/avatar")
    @Operation(summary = "删除用户头像")
    public R<Void> deleteAvatar() {
        try {
            Long userId = userService.getCurrentUserId();
            
            // 获取当前用户信息
            Map<String, Object> profile = userService.getFullProfile();
            String avatar = (String) profile.get("avatar");
            
            // 删除文件
            if (avatar != null && !avatar.isEmpty()) {
                avatarStorageService.deleteAvatar(avatar);
            }
            
            // 清空数据库
            NovelUser user = new NovelUser();
            user.setId(userId);
            user.setAvatar(null);
            userService.updateProfile(user);
            
            log.info("头像删除成功: userId={}", userId);
            return R.ok();
            
        } catch (Exception e) {
            log.error("头像删除异常", e);
            return R.fail("头像删除失败");
        }
    }
    
    /**
     * 修改密码
     * 
     * 需要验证旧密码后才能修改为新密码
     * 
     * @param params 包含oldPassword（旧密码）和newPassword（新密码）
     * @return 操作结果
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return R.fail("密码不能为空");
        }
        
        boolean success = userService.changePassword(oldPassword, newPassword);
        if (success) {
            return R.ok();
        } else {
            return R.fail("旧密码错误");
        }
    }
    
    /**
     * 发送邮箱验证邮件
     * 
     * 向用户的邮箱发送验证链接，验证后邮箱状态变为已验证
     * 
     * @return 发送结果
     */
    @PostMapping("/email/verify")
    @Operation(summary = "发送邮箱验证")
    public R<Void> sendEmailVerification() {
        boolean success = userService.sendEmailVerification();
        if (success) {
            return R.ok();
        } else {
            return R.fail("发送失败");
        }
    }

    /**
     * 获取 VIP 套餐列表（支付接入前为占位配置）
     */
    @GetMapping("/vip/plans")
    @Operation(summary = "获取VIP套餐列表")
    public R<List<Map<String, Object>>> getVipPlans() {
        return R.ok(userService.getVipPlans());
    }

    /**
     * 激活/续费 VIP（模拟支付成功，后续可对接真实支付回调）
     */
    @PostMapping("/vip/activate")
    @Operation(summary = "激活VIP会员")
    public R<Map<String, Object>> activateVip(@RequestBody Map<String, String> params) {
        String planId = params.get("planId");
        if (planId == null || planId.isBlank()) {
            return R.badRequest("套餐不能为空");
        }
        return R.ok(userService.activateVip(planId));
    }
}