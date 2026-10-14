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
import java.util.Optional;

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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthService.class);

    /**
     * Registers a new user with single-query conflict detection.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        long t0 = System.currentTimeMillis();
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        // Single query checks both username and email conflicts simultaneously
        Optional<User> conflictOpt = userRepository.findFirstByUsernameIgnoreCaseOrEmailIgnoreCase(username, email);
        if (conflictOpt.isPresent()) {
            User conflict = conflictOpt.get();
            if (conflict.getUsername() != null && conflict.getUsername().equalsIgnoreCase(username)) {
                return AuthResponse.error("Username '" + username + "' is already taken.");
            }
            if (conflict.getEmail() != null && conflict.getEmail().equalsIgnoreCase(email)) {
                return AuthResponse.error("Email '" + email + "' is already registered.");
            }
        }

        long tDbCheck = System.currentTimeMillis() - t0;
        long tHashStart = System.currentTimeMillis();
        String passwordHash = passwordEncoder.encode(password);
        long tHash = System.currentTimeMillis() - tHashStart;

        User user = new User(username, passwordHash, email);
        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getUsername());
        long totalMs = System.currentTimeMillis() - t0;
        log.info("User registration completed in {}ms [dbCheck: {}ms, bcrypt: {}ms] for username '{}'",
                totalMs, tDbCheck, tHash, username);

        return AuthResponse.success(token, user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates a user with single indexed case-insensitive lookup.
     */
    public AuthResponse login(LoginRequest request) {
        long t0 = System.currentTimeMillis();
        String username = request.getUsername() != null ? request.getUsername().trim() : "";
        String password = request.getPassword() != null ? request.getPassword() : "";

        // Single indexed lookup for username (covers case-insensitive matching)
        User user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
        long tLookup = System.currentTimeMillis() - t0;

        long tMatchStart = System.currentTimeMillis();
        boolean passwordMatches = (user != null) && passwordEncoder.matches(password, user.getPasswordHash());
        long tMatch = System.currentTimeMillis() - tMatchStart;

        if (user == null || !passwordMatches) {
            log.warn("Failed login attempt for username '{}' in {}ms", username, System.currentTimeMillis() - t0);
            return AuthResponse.error("Invalid username or password.");
        }

        String token = jwtUtils.generateToken(user.getUsername());
        long totalMs = System.currentTimeMillis() - t0;
        log.info("User login completed in {}ms [dbLookup: {}ms, bcryptMatch: {}ms] for username '{}'",
                totalMs, tLookup, tMatch, user.getUsername());

        return AuthResponse.success(token, user.getUsername(), user.getEmail());
    }
}
