package dev.yasinkilinc.booking.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

}
