package com.mythweave.web.controller;

import com.mythweave.web.common.R;
import com.mythweave.web.dto.LoginRequest;
import com.mythweave.web.dto.LoginResponse;
import com.mythweave.web.dto.RegisterRequest;
import com.mythweave.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证控制器
 * 
 * 提供用户注册和登录功能：
 * - 用户注册：创建新账户
 * - 用户登录：验证凭证并生成JWT token
 * 
 * 注册和登录接口不需要用户已登录
 */
@Tag(name = "用户认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;

    /**
     * 用户注册
     * 
     * 注册成功后返回成功提示，账户可直接用于登录
     * 
     * @param req 注册信息（包含用户名、密码、邮箱等）
     * @return 注册结果
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<?> register(@RequestBody RegisterRequest req) {
        try {
            authService.register(req);
            return R.ok(Map.of("success", true, "message", "注册成功"));
        } catch (RuntimeException e) {
            // 业务校验异常（如用户名已存在）返回 400，避免前端误判为服务器故障
            return R.badRequest(e.getMessage());
        }
    }

    /**
     * 用户登录
     * 
     * 验证用户名和密码，验证成功后返回JWT token
     * 该token需要在后续请求中通过Authorization header携带
     * 
     * @param req 登录凭证（用户名和密码）
     * @return 登录响应（包含token和用户基本信息）
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@RequestBody LoginRequest req) {
        try {
            return R.ok(authService.login(req));
        } catch (RuntimeException e) {
            // 业务校验异常（如用户名或密码错误）返回 400，避免前端误判为服务器故障
            return R.badRequest(e.getMessage());
        }
    }
}