package org.janggo.whatisjwt.config.security;

import org.janggo.whatisjwt.service.RefreshTokenService;
import org.janggo.whatisjwt.util.jwt.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] WHITE_LIST = {
            "/api/auth/**",
            "/login",
    };

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManager authenticationManager,
                                                   JwtUtil jwtUtil, RefreshTokenService refreshTokenService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().authenticated()  // 나머지는 인증 필요
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterAt(createLoginFilter(authenticationManager, jwtUtil, refreshTokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 로그인 필터 설정 코드 부분
    private static LoginFilter createLoginFilter(AuthenticationManager authenticationManager,
                                                 JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        LoginFilter loginFilter = new LoginFilter(jwtUtil, refreshTokenService);
        loginFilter.setAuthenticationManager(authenticationManager);
        loginFilter.setFilterProcessesUrl("/api/auth/login");
        return loginFilter;
    }
}
