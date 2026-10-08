package com.cubesolve.dto;

import java.util.List;

/**
 * Response DTO for cube validation endpoint.
 */
public class ValidationResponse {

    private boolean valid;
    private List<String> errors;

    public ValidationResponse() {}

    public ValidationResponse(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : List.of();
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
