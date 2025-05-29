package org.janggo.whatisjwt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janggo.whatisjwt.dto.RefreshTokenRequest;
import org.janggo.whatisjwt.dto.SignUpRequest;
import org.janggo.whatisjwt.dto.SignUpResponse;
import org.janggo.whatisjwt.dto.TokenResponse;
import org.janggo.whatisjwt.exception.GlobalExceptionHandler;
import org.janggo.whatisjwt.service.AuthService;
import org.janggo.whatisjwt.service.RefreshTokenService;
import org.janggo.whatisjwt.util.jwt.JwtUtil;
import org.janggo.whatisjwt.util.jwt.RefreshToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.refreshToken();

        log.info("토큰 갱신 요청 - refreshToken: {}", requestRefreshToken.substring(0, 20) + "...");

        try {
            // 1. Refresh Token 유효성 검증
            if (!jwtUtil.validateToken(requestRefreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new GlobalExceptionHandler.ExceptionApiResponse<>(401, "유효하지 않은 Refresh Token입니다.", null));
            }

            // 2. Refresh Token 타입 확인
            String tokenType = jwtUtil.getTokenType(requestRefreshToken);
            if (!"refresh".equals(tokenType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new GlobalExceptionHandler.ExceptionApiResponse<>(401, "RefreshToken이 아닙니다.", null));
            }

            // 3. Refresh Token에서 userId 추출
            Long userId = jwtUtil.getUserIdFromRefreshToken(requestRefreshToken);

            // 4. Redis에서 저장된 Refresh Token 확인
            RefreshToken storedToken = refreshTokenService.findByUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));

            // 5. 요청된 토큰과 저장된 토큰 비교
            if (!requestRefreshToken.equals(storedToken.getRefreshToken())) {
                // 토큰이 다르면 보안상 모든 토큰 삭제
                refreshTokenService.deleteByUserId(userId);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new GlobalExceptionHandler.ExceptionApiResponse<>(401, "토큰이 일치하지 않습니다. 다시 로그인해주세요.", null));
            }

            // 6. 새로운 토큰 발급 (RTR 방식)
            String newAccessToken = jwtUtil.createAccessToken(storedToken.getUsername(), "ROLE_USER");
            String newRefreshToken = jwtUtil.createRefreshToken(userId);

            // 7. 새 Refresh Token을 Redis에 저장
            refreshTokenService.saveRefreshToken(newRefreshToken, userId, storedToken.getUsername());

            log.info("토큰 갱신 성공 - userId: {}", userId);

            TokenResponse tokenResponse = new TokenResponse("Bearer", newAccessToken, newRefreshToken);
            return ResponseEntity.ok(tokenResponse);

        } catch (IllegalArgumentException e) {
            log.warn("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GlobalExceptionHandler.ExceptionApiResponse<>(401, e.getMessage(), null));
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GlobalExceptionHandler.ExceptionApiResponse<>(500, "서버 오류가 발생했습니다.", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        try {
            // Refresh Token 삭제 (로그아웃)
            refreshTokenService.deleteRefreshToken(refreshToken);
            log.info("로그아웃 성공 - refreshToken 삭제 완료");

            return ResponseEntity.ok(new GlobalExceptionHandler.ExceptionApiResponse<>(200, "로그아웃되었습니다.", null));

        } catch (Exception e) {
            log.error("로그아웃 중 오류 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GlobalExceptionHandler.ExceptionApiResponse<>(500, "로그아웃 처리 중 오류가 발생했습니다.", null));
        }
    }
}

