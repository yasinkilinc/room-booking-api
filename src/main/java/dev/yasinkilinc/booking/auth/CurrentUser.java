package dev.yasinkilinc.booking.auth;

import org.springframework.security.core.Authentication;

/** The caller of a request, taken from the validated JWT. */
public record CurrentUser(String username, boolean admin) {

	public static CurrentUser from(Authentication authentication) {
		boolean admin = authentication.getAuthorities()
			.stream()
			.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		return new CurrentUser(authentication.getName(), admin);
	}

	public boolean canManage(String owner) {
		return admin || username.equals(owner);
	}

}
