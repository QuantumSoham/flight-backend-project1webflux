package com.flightapp.service;

import com.flightapp.entity.Airline;
import reactor.core.publisher.Mono;

public interface AirlineService {
    Mono<Airline> addAirline(Airline airline);
    Mono<Airline> findByCode(String code);
}
