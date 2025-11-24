package com.flightapp.service.impl;

import com.flightapp.dto.CreateFlightRequest;
import com.flightapp.dto.SearchRequest;
import com.flightapp.dto.SearchResultDTO;
import com.flightapp.entity.Airline;
import com.flightapp.entity.Flight;
import com.flightapp.repository.AirlineRepository;
import com.flightapp.repository.FlightRepository;
import com.flightapp.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

    private final FlightRepository flightRepository;
    private final AirlineRepository airlineRepository;
    private final ReactiveMongoTemplate template;

    /**
     * Aggregation-based search:
     * - Converts LocalDate -> start/end Instant in Asia/Kolkata zone
     * - Matches fromPlace, toPlace and departureDateTime in [start, end)
     * - Looks up Airline document and unwinds it
     * - Projects fields that map to SearchResultDTO
     *
     * NOTE: this uses collection names "Flight" and "Airline" (case-sensitive) to match your DB.
     * If your collections are named differently, change the collection names below.
     */
    @Override
    public Flux<SearchResultDTO> searchFlights(SearchRequest request) {

        System.out.println("Hit aggregation searchFlights");

        // Convert LocalDate → Instant range in IST
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Instant start = request.getDepartureDate().atStartOfDay(zone).toInstant();
        Instant end = request.getDepartureDate().plusDays(1).atStartOfDay(zone).toInstant();

        MatchOperation match = Aggregation.match(
                Criteria.where("fromPlace").is(request.getFromPlace())
                        .and("toPlace").is(request.getToPlace())
                        .and("departureDateTime").gte(start).lt(end)
        );

        LookupOperation lookupAirline = LookupOperation.newLookup()
                .from("airlines")            // << correct lowercase collection
                .localField("airlineId")
                .foreignField("_id")
                .as("airline");

        Aggregation agg = Aggregation.newAggregation(
                match,
                lookupAirline,
                Aggregation.unwind("airline"),
                Aggregation.project("flightNumber", "departureDateTime", "arrivalDateTime",
                                    "priceOneWay", "priceRoundTrip", "availableSeats")
                        .and("airline.name").as("airlineName")
                        .and("airline.logoUrl").as("airlineLogoUrl")
                        .and("_id").as("flightId")
        );

        // IMPORTANT: lowercase collection name
        return template.aggregate(agg, "flights", SearchResultDTO.class);
    }

//    @Override
//    public reactor.core.publisher.Mono<Void> addInventory(String airlineCode, String flightId) {
//        // Example placeholder if you want a method for adding inventory.
//        return reactor.core.publisher.Mono.empty();
//    }
    @Override
    public reactor.core.publisher.Mono<Void> addInventory(String airlineCode, CreateFlightRequest req) {
        // Find the airline by code, then create & save the flight tied to that airline.
        return airlineRepository.findByCode(airlineCode)
                .switchIfEmpty(reactor.core.publisher.Mono.error(
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Airline with code '" + airlineCode + "' not found"
                        )
                ))
                .flatMap(airline -> {
                    Flight flight = Flight.builder()
                            .airlineId(airline.getId())
                            .flightNumber(req.getFlightNumber())
                            .fromPlace(req.getFromPlace())
                            .toPlace(req.getToPlace())
                            .departureDateTime(req.getDepartureDateTime())
                            .arrivalDateTime(req.getArrivalDateTime())
                            .priceOneWay(req.getPriceOneWay())
                            .priceRoundTrip(req.getPriceRoundTrip())
                            .totalSeats(req.getTotalSeats())
                            .availableSeats(req.getAvailableSeats() != null ? req.getAvailableSeats() : req.getTotalSeats())
                            .build();

                    // If you want to ensure uniqueness (flightNumber + departureDateTime), you can add a check here.
                    return flightRepository.save(flight);
                })
                .then(); // return Mono<Void>
    }

}
