package com.flightapp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

import jakarta.persistence.Id;
import lombok.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection="flight_inventory")
public class FlightInventory 
{
	@Id
	private String id;
	private String airlineName;
	private String airlineLogo;
	private String fromPlace;
	private String toPlace;
	private LocalDateTime departureDateTime;
	private LocalDateTime arrivalDateTime;
	private BigDecimal price;
	private int totalSeats;
	private int seatsAvailable;
	private List<String> seatNumbers;
	
	
	
	
}
