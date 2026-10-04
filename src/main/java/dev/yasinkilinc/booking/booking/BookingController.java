package dev.yasinkilinc.booking.booking;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.yasinkilinc.booking.auth.CurrentUser;
import dev.yasinkilinc.booking.common.PageResponse;

@Tag(name = "Bookings")
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

	private final BookingService service;

	public BookingController(BookingService service) {
		this.service = service;
	}

	@Operation(summary = "Book a room", description = "Returns 409 when the room is already booked at that time.")
	@ApiResponse(responseCode = "201", description = "Booked")
	@ApiResponse(responseCode = "400", description = "Invalid fields or a broken rule (capacity, 15-minute grid, past start)",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "The room is already booked at that time; the body names the conflicting booking",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping
	public ResponseEntity<BookingResponse> create(Authentication auth, @Valid @RequestBody CreateBookingRequest request) {
		BookingResponse booking = service.create(CurrentUser.from(auth), request);
		return ResponseEntity.created(URI.create("/api/bookings/" + booking.id())).body(booking);
	}

	@Operation(summary = "My bookings that have not ended yet, soonest first")
	@GetMapping
	public PageResponse<BookingResponse> upcoming(Authentication auth,
			@PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable) {
		return service.upcoming(CurrentUser.from(auth), pageable);
	}

	@Operation(summary = "Get one of my bookings (admins can get any)")
	@GetMapping("/{id}")
	public BookingResponse get(Authentication auth, @PathVariable Long id) {
		return service.get(CurrentUser.from(auth), id);
	}

	@Operation(summary = "Cancel a booking before it starts (owner or admin)",
			description = "Cancelling an already cancelled booking also returns 204, so retries are safe.")
	@ApiResponse(responseCode = "204", description = "Cancelled")
	@ApiResponse(responseCode = "403", description = "The booking belongs to another user",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "The booking has already started",
			content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> cancel(Authentication auth, @PathVariable Long id) {
		service.cancel(CurrentUser.from(auth), id);
		return ResponseEntity.noContent().build();
	}

}
