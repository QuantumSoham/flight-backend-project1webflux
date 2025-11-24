package com.flightapp.service.impl;

import com.flightapp.entity.Airline;
import com.flightapp.repository.AirlineRepository;
import com.flightapp.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;

    @Override
    public Mono<Airline> addAirline(Airline airline) {
        return airlineRepository.save(airline);
    }

    @Override
    public Mono<Airline> findByCode(String code) {
        return airlineRepository.findByCode(code);
    }
}
