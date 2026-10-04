package dev.yasinkilinc.booking.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.yasinkilinc.booking.common.ApiException;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final UserAccountRepository users;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokens;

	private final String dummyHash;

	public AuthController(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
	}

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	@Operation(summary = "Exchange username and password for a bearer token")
	@PostMapping("/token")
	public TokenResponse token(@Valid @RequestBody LoginRequest request) {
		UserAccount user = users.findByUsername(request.username()).orElse(null);
		// Hash even for unknown users so response time does not reveal which usernames exist
		String hash = user != null ? user.getPasswordHash() : dummyHash;
		boolean matches = passwordEncoder.matches(request.password(), hash);
		if (user == null || !matches) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
		}
		return tokens.issue(user);
	}

}
