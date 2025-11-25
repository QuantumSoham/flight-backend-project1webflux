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
		//here i have used simple save function of mongo , no aggregation used
		return airlineRepository.save(airline);
		
	}

	@Override
	public Mono<Airline> findByCode(String code) {
		//here also i have implemented a simple ,method find by code 
		return airlineRepository.findByCode(code);
	}
}
