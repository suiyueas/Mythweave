package com.novelcraft.web.filter;

import com.novelcraft.web.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 
 * 职责：
 * - 验证请求中的JWT token有效性
 * - 将有效token中的用户ID解析出来，设置到请求属性中
 * - 对需要认证的接口进行拦截，未登录或token无效时返回401错误
 * 
 * 放行的路径：
 * - OPTIONS请求（CORS预检请求）
 * - /api/auth/login（登录接口）
 * - /api/auth/register（注册接口）
 * 
 * 工作流程：
 * 1. 检查是否为OPTIONS请求，是则直接放行
 * 2. 检查是否为登录/注册接口，是则直接放行
 * 3. 从Authorization header中提取Bearer token
 * 4. 验证token有效性
 * 5. token有效则将用户ID设置到request属性中，继续处理
 * 6. token无效或缺失则返回401未授权错误
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;

    /**
     * 过滤器的核心处理方法
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @param chain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // OPTIONS 预检请求直接放行，CORS 头由 CorsFilter 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        if (path.contains("/api/auth/login") || path.contains("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
                chain.doFilter(request, response);
                return;
            }
        }

        // 未认证：写入 CORS 头后再返回 401，防止浏览器只显示 CORS 错误
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
        }
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
    }
}