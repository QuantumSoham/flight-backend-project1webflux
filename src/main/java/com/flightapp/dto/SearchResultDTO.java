package com.flightapp.dto;

import lombok.*;
import java.time.Instant;

@Data
@Builder
//imagine how my response json will look
public class SearchResultDTO 
{
	private String flightId;
	private String flightNumber;
	private String airlineName;
	private String airlineLogoUrl;
	private Instant departureDateTime;
	private Instant arrivalDateTime;
	private Double priceOneWay;
	private Double priceRoundTrip;
	private Integer availableSeats;
	
}
