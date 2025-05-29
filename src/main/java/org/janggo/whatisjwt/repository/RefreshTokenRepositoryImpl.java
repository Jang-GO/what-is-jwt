package org.janggo.whatisjwt.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.janggo.whatisjwt.util.jwt.RefreshToken;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    public final RedisTemplate<String, Object> redisTemplate;
    private static final String REFRESH_PREFIX = "refreshToken:";

    @Override
    public void save(RefreshToken token) {
        String key = REFRESH_PREFIX + token.getUserId();
        redisTemplate.opsForValue().set(key, token, Duration.ofDays(7));
        log.info("✅ Redis 저장: key={}, token={}", key, token.getRefreshToken());
    }

    @Override
    public Optional<RefreshToken> findByUserId(Long userId) {
        String key = REFRESH_PREFIX + userId;
        RefreshToken token = (RefreshToken) redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteByUserId(Long userId) {
        String key = REFRESH_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("🗑️ Redis 삭제 완료: {}", key);
    }
}
