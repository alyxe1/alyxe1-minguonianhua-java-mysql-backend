package org.exh.nianhuawechatminiprogrambackend.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.exh.nianhuawechatminiprogrambackend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT认证拦截器
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求路径
        String path = request.getRequestURI();
        log.debug("JWT拦截器, path: {}", path);

        // 获取Authorization头
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("请求未携带Authorization头或格式错误, path: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未授权：缺少Token\",\"data\":null}");
            return false;
        }

        String token = authorization.substring(7);

        // 验证token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Token验证失败, path: {}", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\",\"data\":null}");
            return false;
        }

        // 从token中获取用户ID并放入request属性中，供后续使用
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);

        log.debug("Token验证成功, userId: {}, path: {}", userId, path);
        return true;
    }
}
