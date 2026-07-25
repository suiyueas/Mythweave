package com.novelcraft.web.controller;

import com.novelcraft.web.common.R;
import com.novelcraft.web.dto.LoginRequest;
import com.novelcraft.web.dto.LoginResponse;
import com.novelcraft.web.dto.RegisterRequest;
import com.novelcraft.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<?> register(@RequestBody RegisterRequest req) {
        try {
            authService.register(req);
            return R.ok(Map.of("success", true, "message", "注册成功"));
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest req) {
        try {
            return R.ok(authService.login(req));
        } catch (RuntimeException e) {
            return R.fail(e.getMessage());
        }
    }
}