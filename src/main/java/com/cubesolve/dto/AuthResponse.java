package com.cubesolve.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Response DTO for auth endpoints. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private String message;
    private boolean success;

    public static AuthResponse success(String token, String username, String email) {
        AuthResponse r = new AuthResponse();
        r.success = true;
        r.token = token;
        r.username = username;
        r.email = email;
        r.message = "Authentication successful.";
        return r;
    }

    public static AuthResponse error(String message) {
        AuthResponse r = new AuthResponse();
        r.success = false;
        r.message = message;
        return r;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
