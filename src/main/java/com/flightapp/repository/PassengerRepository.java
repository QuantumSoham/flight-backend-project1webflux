package com.flightapp.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.flightapp.entity.Passenger;

import reactor.core.publisher.Flux;

public interface PassengerRepository extends ReactiveMongoRepository<Passenger, String> 
{
	Flux<Passenger> findByBookingId(String bookingId);
	Flux<Passenger> findByFlightId(String flightId);
}
