package com.securehandoff.securehandoff.service;

import java.util.HashSet;

import org.apache.kafka.common.errors.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.securehandoff.securehandoff.repository.UserRepository;
import com.securehandoff.dto.AuthResponse;
import com.securehandoff.dto.LoginRequest;
import com.securehandoff.dto.RegisterRequest;
import com.securehandoff.exception.ApiException;
import com.securehandoff.model.Role;
import com.securehandoff.model.User;
import com.securehandoff.repository.UserRepository;
import com.securehandoff.security.JwtUtil;
import com.securehandoff.security.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Set;

@Service
@RequiredArgsConstructor

public class AuthService {
      private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final LoginRateLimiter loginRateLimiter;

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(Set.of(Role.OWNER)))
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateAccessToken(user);
        return AuthResponse.of(token, user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        loginRateLimiter.checkAllowed(email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (Exception ex) {
            loginRateLimiter.recordFailedAttempt(email);
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        loginRateLimiter.recordSuccess(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        String token = jwtUtil.generateAccessToken(user);
        return AuthResponse.of(token, user.getEmail(), user.getFullName());
    }

}
