package com.mythweave.web.service;

import com.mythweave.web.dto.LoginRequest;
import com.mythweave.web.dto.LoginResponse;
import com.mythweave.web.dto.RegisterRequest;
import com.mythweave.web.entity.NovelUser;
import com.mythweave.web.mapper.NovelUserMapper;
import com.mythweave.web.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务
 * 处理用户注册、登录等认证相关业务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final NovelUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     *
     * @param req 注册请求（用户名、密码、邮箱）
     * @return 创建的用户对象
     * @throws RuntimeException 用户名已存在时抛出
     */
    @Transactional
    public NovelUser register(RegisterRequest req) {
        // 检查用户名是否已存在
        if (userMapper.selectByUsername(req.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        NovelUser user = new NovelUser();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));  // 密码加密存储
        user.setNickname(req.getUsername());                   // 默认昵称同用户名
        user.setEmail(req.getEmail());
        user.setRole("user");          // 默认角色为普通用户
        user.setVipLevel(0);          // 默认无VIP
        user.setBio("");              // 空简介
        userMapper.insert(user);
        return user;
    }

    /**
     * 用户登录（支持用户名或邮箱登录）
     * 登录成功后自动检查并补全管理员VIP
     *
     * @param req 登录请求
     * @return 登录响应（包含JWT token和用户信息）
     * @throws RuntimeException 认证失败时抛出
     */
    public LoginResponse login(LoginRequest req) {
        String usernameOrEmail = req.getUsername();
        NovelUser user = null;

        // 根据输入内容判断是用户名还是邮箱登录
        if (usernameOrEmail.contains("@")) {
            user = userMapper.selectByEmail(usernameOrEmail);
        } else {
            user = userMapper.selectByUsername(usernameOrEmail);
        }

        // 验证密码
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 管理员VIP自动补全
        ensureAdminVip(user);

        // 构建登录响应
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

    /**
     * 计算 VIP 状态
     *
     * @param user 用户对象
     * @return VIP状态：active-生效中 / expired-已过期 / none-未开通
     */
    private String calcVipStatus(NovelUser user) {
        Integer level = user.getVipLevel();
        if (level == null || level <= 0) return "none";
        if (user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(LocalDateTime.now())) {
            return "active";
        }
        return "expired";
    }

    /**
     * 管理员VIP自动补全/续期
     * 规则：
     * - role=admin 的用户自动享受 VIP 钻石待遇（永久有效）
     * - 兼容旧库：username=admin 的历史用户自动升级为 admin 角色
     *
     * @param user 用户对象
     */
    private void ensureAdminVip(NovelUser user) {
        if (user == null) return;

        // 检查是否为管理员角色
        if (!"admin".equals(user.getRole())) {
            // 历史兼容：用户名 admin 的用户自动升级为 admin 角色
            if ("admin".equals(user.getUsername())) {
                user.setRole("admin");
                userMapper.updateById(user);
            } else {
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        // 检查是否需要补全/续期 VIP
        boolean needVip = user.getVipLevel() == null || user.getVipLevel() <= 0
                || user.getVipExpireAt() == null || !user.getVipExpireAt().isAfter(now);
        if (!needVip) return;

        // 补全 VIP 信息
        user.setVipLevel(3);  // 钻石等级
        user.setVipExpireAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59));  // 永久有效
        if (user.getVipPurchasedAt() == null) {
            user.setVipPurchasedAt(now);
        }
        userMapper.updateById(user);
    }
}