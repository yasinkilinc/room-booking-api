package dev.yasinkilinc.booking.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * A business error that becomes an RFC 9457 problem response.
 * {@code properties} are added to the response body, for example the id of a conflicting booking.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;

	private final Map<String, Object> properties;

	public ApiException(HttpStatus status, String message) {
		this(status, message, Map.of());
	}

	public ApiException(HttpStatus status, String message, Map<String, Object> properties) {
		super(message);
		this.status = status;
		this.properties = properties;
	}

	public static ApiException notFound(String what, Object id) {
		return new ApiException(HttpStatus.NOT_FOUND, what + " " + id + " does not exist");
	}

	public static ApiException badRequest(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, message);
	}

	public static ApiException conflict(String message, Map<String, Object> properties) {
		return new ApiException(HttpStatus.CONFLICT, message, properties);
	}

	public HttpStatus status() {
		return status;
	}

	public Map<String, Object> properties() {
		return properties;
	}

}
