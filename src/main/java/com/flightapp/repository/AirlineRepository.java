package com.flightapp.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.flightapp.entity.Airline;

import reactor.core.publisher.Mono;

public interface AirlineRepository extends ReactiveMongoRepository<Airline,String>{

	Mono<Airline> findByCode(String code);
}
