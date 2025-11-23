package com.flightapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id; // booking_id
    private String pnr; // unique
    private String flightId;
    private String userId; // nullable
    private String userName;
    private String userEmail;
    private Integer numberOfSeats;
    private Instant bookingDateTime;
    private Instant journeyDateTime;
    private BookingStatus status;
    // store passenger ids inline or as embedded docs; we will store passengers as separate collection
}
