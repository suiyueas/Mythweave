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
        userMapper.insert(user);
        return user;
    }

    public LoginResponse login(LoginRequest req) {
        NovelUser user = userMapper.selectByUsername(req.getUsername());
        if (user == null || !encoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        return LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getId(), user.getUsername()))
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }
}