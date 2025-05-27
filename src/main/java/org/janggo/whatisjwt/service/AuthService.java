package org.janggo.whatisjwt.service;

import org.janggo.whatisjwt.dto.SignUpRequest;
import org.janggo.whatisjwt.dto.SignUpResponse;

public interface AuthService {
    SignUpResponse signUp(final SignUpRequest signUpRequest);
}
