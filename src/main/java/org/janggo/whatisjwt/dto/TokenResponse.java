package org.janggo.whatisjwt.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken
) {}
