package dev.vuemind.api.auth.dto;

public record LoginResponse(String accessToken, UserDto user) {
}
