package com.flightapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class BookingResponse {
    private String pnr;
    private String flightId;
    private String userEmail;
    private int numberOfSeats;
    private Instant bookingDateTime;
    private String status;
    private String bookingId;
}
