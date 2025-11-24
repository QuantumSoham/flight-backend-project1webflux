package com.flightapp.service;

import com.flightapp.dto.CreateFlightRequest;
import com.flightapp.dto.SearchRequest;
import com.flightapp.dto.SearchResultDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FlightService {
    Flux<SearchResultDTO> searchFlights(SearchRequest request);
    // admin add inventory
//    reactor.core.publisher.Mono<Void> addInventory(String airlineCode, String flightId); // example
    Mono<Void> addInventory(String airlineCode, CreateFlightRequest request);
}
