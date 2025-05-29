package org.janggo.whatisjwt.util.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshToken implements Serializable {

    private String refreshToken;
    private Long userId;
    private String username;
}
