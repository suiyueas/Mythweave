package com.novelcraft.web.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novelcraft.web.entity.NovelUser;
import com.novelcraft.web.entity.NovelUserStats;
import com.novelcraft.web.mapper.NovelUserMapper;
import com.novelcraft.web.mapper.NovelUserStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<NovelUserMapper, NovelUser> {
    
    private final NovelUserStatsMapper userStatsMapper;
    
    /**
     * 获取用户信息（默认用户ID=1）
     */
    public NovelUser getProfile() {
        return getById(1L);
    }
    
    /**
     * 更新用户信息
     */
    @Transactional
    public void updateProfile(NovelUser user) {
        user.setId(1L);
        updateById(user);
    }
    
    /**
     * 获取用户统计
     */
    public NovelUserStats getStats() {
        return userStatsMapper.selectById(1L);
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
            result.put("emailVerified", user.getEmailVerified());
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
    
    /**
     * 修改密码
     */
    @Transactional
    public boolean changePassword(String oldPassword, String newPassword) {
        NovelUser user = getById(1L);
        if (user == null) {
            return false;
        }
        // 简单密码验证（实际项目应使用加密）
        if (!user.getPassword().equals(oldPassword)) {
            return false;
        }
        user.setPassword(newPassword);
        return updateById(user);
    }
    
    /**
     * 发送邮箱验证（模拟）
     */
    public boolean sendEmailVerification() {
        NovelUser user = getById(1L);
        if (user == null || user.getEmail() == null) {
            return false;
        }
        // 实际项目应发送验证邮件
        return true;
    }
}
