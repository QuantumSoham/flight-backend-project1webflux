package com.flightapp.dto;
import lombok.Data;
import java.time.Instant;

//imagine how my request json would look like
@Data
public class SearchRequest 
{
  private String fromPlace;
  private String toPlace;
  private java.time.LocalDate departureDate; // 
  private boolean roundTrip;
  private Instant returnDate;//i am keeping this as a optional param
}
