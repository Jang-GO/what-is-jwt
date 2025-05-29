package org.janggo.whatisjwt.repository;

import org.janggo.whatisjwt.util.jwt.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository{
    void save(RefreshToken token);
    Optional<RefreshToken> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
