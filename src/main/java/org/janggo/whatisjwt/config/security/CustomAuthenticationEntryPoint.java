package org.janggo.whatisjwt.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.warn("인증 실패 - URL: {}, IP: {}, 사유: {}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                authException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // JWT 토큰 관련 오류 구분
        String errorType = "AUTHENTICATION_FAILED";
        String message = "인증이 필요합니다.";

        if (authException.getMessage().contains("JWT")) {
            errorType = "INVALID_TOKEN";
            message = "유효하지 않은 토큰입니다.";
        } else if (authException.getMessage().contains("expired")) {
            errorType = "TOKEN_EXPIRED";
            message = "토큰이 만료되었습니다.";
        }

        // JSON 응답 생성
        String jsonResponse = """
            {
                "error": "%s",
                "message": "%s",
                "status": 401,
                "timestamp": "%s",
                "path": "%s"
            }
            """.formatted(
                errorType,
                message,
                Instant.now().toString(),
                request.getRequestURI()
        );

        response.getWriter().write(jsonResponse);
    }
}
