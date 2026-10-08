package com.cubesolve.service;

import com.cubesolve.dto.AuthResponse;
import com.cubesolve.dto.LoginRequest;
import com.cubesolve.dto.RegisterRequest;
import com.cubesolve.model.User;
import com.cubesolve.repository.UserRepository;
import com.cubesolve.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user registration and login.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    /**
     * Registers a new user.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.error("Username '" + request.getUsername() + "' is already taken.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.error("Email '" + request.getEmail() + "' is already registered.");
        }

        User user = new User(
            request.getUsername(),
            passwordEncoder.encode(request.getPassword()),
            request.getEmail()
        );
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername());
        return AuthResponse.success(token, user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates a user and returns a JWT token.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
            .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return AuthResponse.error("Invalid username or password.");
        }

        String token = jwtUtils.generateToken(user.getUsername());
        return AuthResponse.success(token, user.getUsername(), user.getEmail());
    }
}
