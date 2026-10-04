package dev.yasinkilinc.booking.booking;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(@NotNull Long roomId, @NotBlank @Size(max = 120) String title,
		@NotNull @Min(1) Integer attendees, @NotNull Instant startsAt, @NotNull Instant endsAt) {

}
