package com.exptrack.user.dto;

public record RegistrationRequest(String email, String password, String defaultCurrency) {
}
