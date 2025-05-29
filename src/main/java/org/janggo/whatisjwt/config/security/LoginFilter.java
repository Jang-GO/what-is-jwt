package org.janggo.whatisjwt.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janggo.whatisjwt.dto.LoginResponse;
import org.janggo.whatisjwt.dto.TokenResponse;
import org.janggo.whatisjwt.exception.GlobalExceptionHandler;
import org.janggo.whatisjwt.service.RefreshTokenService;
import org.janggo.whatisjwt.util.jwt.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String username = obtainUsername(request);
        String password = obtainPassword(request);

        log.info("로그인 시도(username) : {}", username);
        log.info("로그인 시도(password) : {}", password);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);

        return this.getAuthenticationManager().authenticate(authToken);
    }

    // 로그인 성공시 실행하는 메소드
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();

        String username = userDetails.getUsername();
        String role = userDetails.getAuthorities().iterator().next().getAuthority();
        Long userId = userDetails.getId();
        // JWT 토큰 생성
        String accessToken = jwtUtil.createAccessToken(username, role);
        String refreshToken = jwtUtil.createRefreshToken(userDetails.getId());

        // Redis에 Refresh Token 저장 (RTR 방식)
        log.info("저장 전: refreshToken={}, userId={}, username={}", refreshToken, userId, username);
        refreshTokenService.saveRefreshToken(refreshToken, userId, username);

        // 응답 객체 생성
        TokenResponse tokenResponse = new TokenResponse("Bearer", accessToken, refreshToken);
        LoginResponse loginResponse = new LoginResponse("로그인 성공", tokenResponse);

        // JSON 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.OK.value());

        // JSON 응답 전송
        objectMapper.writeValue(response.getWriter(), loginResponse);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        log.info("로그인 failure: {}", failed.getMessage());

        String customMessage = findMessage(failed);

        // 에러 응답 객체 생성
        GlobalExceptionHandler.ExceptionApiResponse<Void> errorResponse = new GlobalExceptionHandler.ExceptionApiResponse<>(
                HttpStatus.UNAUTHORIZED.value(),
                customMessage,
                null
        );

        // JSON 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // JSON 응답 전송
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private static String findMessage(AuthenticationException failed) {
        if (failed instanceof BadCredentialsException) {
            return "아이디 또는 비밀번호가 올바르지 않습니다.";
        } else if (failed instanceof DisabledException) {
            return "계정이 비활성화되었습니다.";
        } else if (failed instanceof LockedException) {
            return "계정이 잠겨있습니다.";
        } else {
            return "로그인에 실패했습니다.";
        }
    }
}
