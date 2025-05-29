package org.janggo.whatisjwt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janggo.whatisjwt.dto.TokenResponse;
import org.janggo.whatisjwt.util.jwt.JwtUtil;
import org.janggo.whatisjwt.util.jwt.RefreshToken;
import org.janggo.whatisjwt.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    // Refresh Token 저장
    public void saveRefreshToken(String refreshToken, Long userId, String username) {
        // 기존 토큰이 있다면 삭제 (RTR 방식)
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken token = new RefreshToken(refreshToken, userId, username);
        refreshTokenRepository.save(token);
    }

    // 사용자 ID로 Refresh Token 조회
    public Optional<RefreshToken> findByUserId(Long userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    // 사용자 ID로 Refresh Token 삭제
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("사용자의 모든 Refresh Token 삭제 완료 - userId: {}", userId);
    }

    public void deleteRefreshToken(String refreshToken) {
        try {
            // 1. Refresh Token에서 userId 추출
            Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);

            // 2. userId로 삭제 (더 효율적)
            refreshTokenRepository.deleteByUserId(userId);

            log.info("Refresh Token 삭제 완료 - userId: {}", userId);

        } catch (Exception e) {
            log.error("Refresh Token 삭제 중 오류: ", e);
            // 토큰이 유효하지 않더라도 예외를 던지지 않음 (이미 로그아웃 상태로 간주)
        }
    }

    // 토큰 갱신 로직
    public TokenResponse refreshAccessToken(String requestRefreshToken) {
        // 1. Refresh Token 유효성 검증
        if (!jwtUtil.validateToken(requestRefreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 RefreshToken입니다.");
        }

        // 2. Refresh Token 타입 확인
        String tokenType = jwtUtil.getTokenType(requestRefreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("Refresh Token이 아닙니다.");
        }

        // 3. Refresh Token에서 userId 추출
        Long userId = jwtUtil.getUserIdFromRefreshToken(requestRefreshToken);

        // 4. Redis에서 저장된 Refresh Token 확인
        RefreshToken storedToken = findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 Refresh Token입니다."));

        // 5. 요청된 토큰과 저장된 토큰 비교
        if (!requestRefreshToken.equals(storedToken.getRefreshToken())) {
            // 토큰이 다르면 보안상 모든 토큰 삭제
            deleteByUserId(userId);
            throw new IllegalArgumentException("토큰이 일치하지 않습니다. 다시 로그인해주세요.");
        }

        // 6. 새로운 토큰 발급 (RTR 방식)
        String newAccessToken = jwtUtil.createAccessToken(storedToken.getUsername(), "ROLE_USER");
        String newRefreshToken = jwtUtil.createRefreshToken(userId);

        // 7. 새 Refresh Token을 Redis에 저장
        saveRefreshToken(newRefreshToken, userId, storedToken.getUsername());

        log.info("토큰 갱신 성공 - userId: {}", userId);

        return new TokenResponse("Bearer", newAccessToken, newRefreshToken);
    }
}
