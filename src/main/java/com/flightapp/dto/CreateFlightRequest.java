package com.flightapp.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFlightRequest {

	@NotBlank
	private String flightNumber;

	@NotBlank
	private String fromPlace;

	@NotBlank
	private String toPlace;

	@NotNull
	private Instant departureDateTime;

	@NotNull
	private Instant arrivalDateTime;

	@NotNull
	@PositiveOrZero
	private Double priceOneWay;

	@NotNull
	@PositiveOrZero
	private Double priceRoundTrip;

	@NotNull
	@Positive
	private Integer totalSeats;

	// optional: if not provided we will set availableSeats = totalSeats
	private Integer availableSeats;
}
