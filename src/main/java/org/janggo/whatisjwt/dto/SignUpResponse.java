package org.janggo.whatisjwt.dto;

public record SignUpResponse(
        String message,
        String username,
        String email
) {
}
