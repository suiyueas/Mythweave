package com.novelcraft.web.service;

import com.novelcraft.web.dto.LoginRequest;
import com.novelcraft.web.dto.LoginResponse;
import com.novelcraft.web.dto.RegisterRequest;
import com.novelcraft.web.entity.NovelUser;
import com.novelcraft.web.mapper.NovelUserMapper;
import com.novelcraft.web.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final NovelUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Transactional
    public NovelUser register(RegisterRequest req) {
        if (userMapper.selectByUsername(req.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }
        NovelUser user = new NovelUser();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setNickname(req.getUsername());
        user.setEmail(req.getEmail());
        // 新用户默认角色为普通用户，VIP=0，空简介、无头像
        user.setRole("user");
        user.setVipLevel(0);
        user.setBio("");
        userMapper.insert(user);
        return user;
    }

    public LoginResponse login(LoginRequest req) {
        NovelUser user = userMapper.selectByUsername(req.getUsername());
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        ensureAdminVip(user);
        return LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getId(), user.getUsername()))
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .role(user.getRole())
                        .vipLevel(user.getVipLevel())
                        .vipExpireAt(user.getVipExpireAt())
                        .vipPurchasedAt(user.getVipPurchasedAt())
                        .vipStatus(calcVipStatus(user))
                        .build())
                .build();
    }

    /** 计算 VIP 状态：active-生效中 / expired-已过期 / none-未开通 */
    private String calcVipStatus(NovelUser user) {
        Integer level = user.getVipLevel();
        if (level == null || level <= 0) return "none";
        if (user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(java.time.LocalDateTime.now())) {
            return "active";
        }
        return "expired";
    }

    /**
     * 管理员默认 VIP：role=admin 的用户自动补全为钻石等级（永久有效）。
     * 兼容旧库：username=admin 的历史用户自动升级为 admin 角色并持久化。
     */
    private void ensureAdminVip(NovelUser user) {
        if (user == null) return;
        if (!"admin".equals(user.getRole())) {
            if ("admin".equals(user.getUsername())) {
                user.setRole("admin");
                userMapper.updateById(user);
            } else {
                return;
            }
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        boolean needVip = user.getVipLevel() == null || user.getVipLevel() <= 0
                || user.getVipExpireAt() == null || !user.getVipExpireAt().isAfter(now);
        if (!needVip) return;
        user.setVipLevel(3);
        user.setVipExpireAt(java.time.LocalDateTime.of(2099, 12, 31, 23, 59, 59));
        if (user.getVipPurchasedAt() == null) {
            user.setVipPurchasedAt(now);
        }
        userMapper.updateById(user);
    }
}