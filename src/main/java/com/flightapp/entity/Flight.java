package com.flightapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "flights")
public class Flight {
	@Id
	private String id; // flight_id
	private String airlineId;
	private String flightNumber;
	private String fromPlace;
	private String toPlace;
	private Instant departureDateTime;
	private Instant arrivalDateTime;
	private Double priceOneWay;
	private Double priceRoundTrip;
	private Integer totalSeats;
	private Integer availableSeats;
}
