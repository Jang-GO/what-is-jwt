package org.janggo.whatisjwt.config.security;

import org.janggo.whatisjwt.service.RefreshTokenService;
import org.janggo.whatisjwt.util.jwt.JwtAuthenticationFilter;
import org.janggo.whatisjwt.util.jwt.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] WHITE_LIST = {
            "/api/auth/**",
            "/login",
            "/index.html",
            "/jwt-manager.js",
            "/",
            "favicon.ico",
            "/api/test/public"
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
                                                   JwtUtil jwtUtil, RefreshTokenService refreshTokenService,
                                                   CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
                                                   CustomAccessDeniedHandler customAccessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().authenticated()  // 나머지는 인증 필요
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(createJwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                .addFilterAt(createLoginFilter(authenticationManager, jwtUtil, refreshTokenService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> {
                    ex.accessDeniedHandler(customAccessDeniedHandler);
                    ex.authenticationEntryPoint(customAuthenticationEntryPoint);
                });

        return http.build();
    }

    // 로그인 필터 설정 코드 부분
    private static LoginFilter createLoginFilter(AuthenticationManager authenticationManager, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        LoginFilter loginFilter = new LoginFilter(jwtUtil, refreshTokenService);
        loginFilter.setAuthenticationManager(authenticationManager);
        loginFilter.setFilterProcessesUrl("/api/auth/login");
        return loginFilter;
    }

    private static JwtAuthenticationFilter createJwtAuthenticationFilter(JwtUtil jwtUtil){
        return new JwtAuthenticationFilter(jwtUtil);
    }
}
