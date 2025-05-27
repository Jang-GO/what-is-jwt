package org.janggo.whatisjwt.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.janggo.whatisjwt.dto.SignUpRequest;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name="users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="username", unique = true, nullable = false)
    private String username;

    @Column(name="password", nullable = false)
    private String password;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public static User from(SignUpRequest request, String encodedPassword) {
        return User.builder()
                .username(request.username())
                .password(encodedPassword)
                .email(request.email())
                .role(UserRole.USER)
                .build();
    }
}
