package org.janggo.whatisjwt.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.janggo.whatisjwt.dto.SignUpRequest;
import org.janggo.whatisjwt.dto.SignUpResponse;
import org.janggo.whatisjwt.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authService.signUp(request);
        return ResponseEntity.ok(response);
    }
}

