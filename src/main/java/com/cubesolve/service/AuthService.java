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
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByUsername(username)) {
            return AuthResponse.error("Username '" + username + "' is already taken.");
        }
        if (userRepository.existsByEmailIgnoreCase(email) || userRepository.existsByEmail(email)) {
            return AuthResponse.error("Email '" + email + "' is already registered.");
        }

        User user = new User(
            username,
            passwordEncoder.encode(password),
            email
        );
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername());
        return AuthResponse.success(token, user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates a user and returns a JWT token.
     */
    public AuthResponse login(LoginRequest request) {
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        User user = userRepository.findByUsernameIgnoreCase(username)
            .orElseGet(() -> userRepository.findByUsername(username).orElse(null));

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return AuthResponse.error("Invalid username or password.");
        }

        String token = jwtUtils.generateToken(user.getUsername());
        return AuthResponse.success(token, user.getUsername(), user.getEmail());
    }
}
