package dev.yasinkilinc.booking.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("booking.jwt")
public record JwtProperties(String secret, @NotNull Duration ttl) {

	@AssertTrue(message = "booking.jwt.secret (env JWT_SECRET) must be set and at least 32 bytes long")
	public boolean isSecretLongEnough() {
		return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
	}

}
