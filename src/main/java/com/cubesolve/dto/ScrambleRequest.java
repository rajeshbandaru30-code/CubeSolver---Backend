package com.cubesolve.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Optional request DTO for scramble generation endpoint.
 */
public class ScrambleRequest {

    @Min(value = 1, message = "Scramble length must be at least 1 move.")
    @Max(value = 100, message = "Scramble length cannot exceed 100 moves.")
    private Integer length = 20;

    public ScrambleRequest() {}

    public ScrambleRequest(Integer length) {
        if (length != null) {
            this.length = length;
        }
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }
}
