package com.flightapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

import com.flightapp.entity.BookingStatus;

@Data
@Builder
public class TicketDTO {
    private String pnr;
    private String flightId;
    private String flightNumber;
    private String airlineName;
    private String userName;
    private String userEmail;
    private Instant journeyDateTime;
    private BookingStatus status;
    private List<PassengerDTO> passengers;
}
