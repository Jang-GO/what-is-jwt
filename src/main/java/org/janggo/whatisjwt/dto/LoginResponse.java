package org.janggo.whatisjwt.dto;

public record LoginResponse(
        String message,
        TokenResponse tokenResponse
) { }
