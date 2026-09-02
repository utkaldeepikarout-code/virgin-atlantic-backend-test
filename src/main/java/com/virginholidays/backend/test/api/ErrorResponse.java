package com.virginholidays.backend.test.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stable API error payload for client-visible validation failures.
 */
public record ErrorResponse(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("details") String details
) {
}

