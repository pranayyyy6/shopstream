package com.shopstream.auth.service;

import com.shopstream.auth.dto.*;
import com.shopstream.auth.model.Role;
import com.shopstream.auth.model.User;
import com.shopstream.auth.repository.UserRepository;
import com.shopstream.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                // BCrypt hash the password — never store plain text
                .password(passwordEncoder.encode(request.getPassword()))
                // First user registered becomes ADMIN for demo
                // In production: separate admin creation flow
                .role(userRepository.count() == 0 ? Role.ADMIN : Role.USER)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: {} with role: {}", saved.getEmail(), saved.getRole());

        String token = jwtService.generateToken(saved);

        return AuthResponse.builder()
                .token(token)
                .email(saved.getEmail())
                .name(saved.getName())
                .role(saved.getRole().name())
                .expiresIn(jwtService.getExpiration())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        /*
         * AuthenticationManager handles the full authentication.
         * It loads the user, checks the password with BCrypt,
         * throws BadCredentialsException if wrong.
         * No manual password checking needed.
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);
        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpiration())
                .build();
    }
}