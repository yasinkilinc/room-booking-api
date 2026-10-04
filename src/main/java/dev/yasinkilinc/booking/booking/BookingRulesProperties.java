package dev.yasinkilinc.booking.booking;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("booking.rules")
public record BookingRulesProperties(@Min(1) int slotMinutes, @NotNull Duration maxDuration, @Min(1) int maxDaysAhead) {

}
