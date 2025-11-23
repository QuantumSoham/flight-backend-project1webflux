package com.flightapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BookingRequest {
    @NotBlank
    private String userName;
    @Email
    private String userEmail;
    @Min(1)
    private int numberOfSeats;
    @NotEmpty
    private List<PassengerDTO> passengers;
    private boolean loggedInUser; // optional: whether booking by logged-in user
    private String userId; // optional
}
