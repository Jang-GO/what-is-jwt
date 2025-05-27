package org.janggo.whatisjwt.service;

import lombok.RequiredArgsConstructor;
import org.janggo.whatisjwt.domain.User;
import org.janggo.whatisjwt.dto.SignUpRequest;
import org.janggo.whatisjwt.dto.SignUpResponse;
import org.janggo.whatisjwt.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SignUpResponse signUp(final SignUpRequest request) {
        checkDuplicateUser(request);

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.from(request, encodedPassword);

        User savedUser = userRepository.save(user);

        return new SignUpResponse("회원가입 성공"
        , savedUser.getUsername(), savedUser.getEmail());
    }

    private void checkDuplicateUser(final SignUpRequest request) {
        // 중복 검사
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다");
        }
    }
}
