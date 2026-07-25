package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.entity.NovelUser;
import com.novelcraft.web.entity.NovelUserStats;
import com.novelcraft.web.service.AvatarStorageService;
import com.novelcraft.web.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final AvatarStorageService avatarStorageService;
    
    /**
     * 获取用户信息
     */
    @GetMapping("/profile")
    public R<Map<String, Object>> getProfile() {
        return R.ok(userService.getFullProfile());
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody NovelUser user) {
        userService.updateProfile(user);
        return R.ok();
    }
    
    /**
     * 获取用户统计
     */
    @GetMapping("/stats")
    public R<NovelUserStats> getStats() {
        return R.ok(userService.getStats());
    }
    
    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    public R<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 获取当前用户ID（暂时使用默认用户ID 1）
            Long userId = 1L;
            
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
     * 删除头像
     */
    @DeleteMapping("/avatar")
    public R<Void> deleteAvatar() {
        try {
            Long userId = 1L;
            
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
     */
    @PutMapping("/password")
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
     * 发送邮箱验证
     */
    @PostMapping("/email/verify")
    public R<Void> sendEmailVerification() {
        boolean success = userService.sendEmailVerification();
        if (success) {
            return R.ok();
        } else {
            return R.fail("发送失败");
        }
    }
}