package com.exptrack.user.dto;

import java.time.Instant;

public record SessionResponse(String email, String defaultCurrency, Instant createdAt) {
}
