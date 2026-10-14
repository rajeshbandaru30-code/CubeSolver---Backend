package com.cubesolve.controller;

import com.cubesolve.dto.AuthResponse;
import com.cubesolve.dto.LoginRequest;
import com.cubesolve.dto.RegisterRequest;
import com.cubesolve.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication (register, login).
 * No business logic — delegates to AuthService.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * Registers a new user. Returns a JWT token on success with Server-Timing header.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        long t0 = System.currentTimeMillis();
        AuthResponse response = authService.register(request);
        long dur = System.currentTimeMillis() - t0;
        return (response.isSuccess()
            ? ResponseEntity.ok()
            : ResponseEntity.badRequest())
            .header("Server-Timing", "app;dur=" + dur + ";desc=\"Registration Processing\"")
            .body(response);
    }

    /**
     * POST /api/auth/login
     * Authenticates a user. Returns a JWT token on success with Server-Timing header.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        long t0 = System.currentTimeMillis();
        AuthResponse response = authService.login(request);
        long dur = System.currentTimeMillis() - t0;
        return (response.isSuccess()
            ? ResponseEntity.ok()
            : ResponseEntity.status(401))
            .header("Server-Timing", "app;dur=" + dur + ";desc=\"Login Processing\"")
            .body(response);
    }
}
