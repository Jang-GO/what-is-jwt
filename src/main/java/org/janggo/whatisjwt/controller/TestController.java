package org.janggo.whatisjwt.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    @GetMapping("/protected")
    public ResponseEntity<String> protectedEndpoint(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok("✅ 보호된 API 접근 성공! 사용자: " + username +
                ", 시간: " + LocalDateTime.now());
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(Authentication authentication) {
        Map<String, Object> userInfo = Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities(),
                "authenticated", authentication.isAuthenticated(),
                "accessTime", LocalDateTime.now()
        );
        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/create-post")
    public ResponseEntity<Map<String, Object>> createPost(@RequestBody Map<String, String> request,
                                                          Authentication authentication) {
        String title = request.get("title");
        String content = request.get("content");
        String author = authentication.getName();

        Map<String, Object> post = Map.of(
                "id", System.currentTimeMillis(),
                "title", title,
                "content", content,
                "author", author,
                "createdAt", LocalDateTime.now()
        );

        return ResponseEntity.ok(post);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminEndpoint(Authentication authentication) {
        return ResponseEntity.ok("🔐 관리자 전용 API 접근 성공! 관리자: " + authentication.getName());
    }

    @DeleteMapping("/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId, Authentication authentication) {

        String adminName = authentication.getName();

        // 삭제 시작 로그
        log.info("사용자 삭제 요청 시작 - 대상 사용자 ID: {}, 요청 관리자: {}", userId, adminName);

        try {
            // 실제 삭제 로직이 여기에 들어갈 예정
            // userService.deleteUser(userId);

            // 삭제 성공 로그
            log.info("사용자 삭제 완료 - 사용자 ID: {}, 관리자: {}", userId, adminName);

            return ResponseEntity.ok("🗑️ 사용자 " + userId + " 삭제 완료 (관리자: " + adminName + ")");

        } catch (Exception e) {
            // 삭제 실패 로그
            log.error("사용자 삭제 실패 - 사용자 ID: {}, 관리자: {}, 오류: {}",
                    userId, adminName, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ 사용자 삭제 중 오류가 발생했습니다.");
        }
    }

    @GetMapping("/public")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("🌍 공개 API - 인증 불필요, 시간: " + LocalDateTime.now());
    }
}
