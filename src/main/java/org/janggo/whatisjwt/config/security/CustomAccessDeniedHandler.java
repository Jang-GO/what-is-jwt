package org.janggo.whatisjwt.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Component
@Slf4j
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("접근 거부 - URL: {}, 사용자: {}, 사유: {}",
                request.getRequestURI(),
                request.getRemoteUser(),
                accessDeniedException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // JSON 응답 생성
        String jsonResponse = """
            {
                "error": "ACCESS_DENIED",
                "message": "접근 권한이 없습니다.",
                "status": 403,
                "timestamp": "%s",
                "path": "%s"
            }
            """.formatted(
                Instant.now().toString(),
                request.getRequestURI()
        );

        response.getWriter().write(jsonResponse);
    }
}

