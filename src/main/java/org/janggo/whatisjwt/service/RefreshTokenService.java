package org.janggo.whatisjwt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
