package com.flightapp.repository;

import java.time.Instant;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.flightapp.entity.Flight;

import reactor.core.publisher.Flux;

public interface FlightRepository extends ReactiveMongoRepository<Flight, String> {
	Flux<Flight> findByFromPlaceAndToPlaceAndDepartureDateTimeBetween(String from, String to, Instant start,
			Instant end);
}
