package com.flightapp.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.flightapp.dto.BookingResponse;
import com.flightapp.entity.Booking;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookingRepository extends ReactiveMongoRepository<Booking,String>
{
	Mono<Booking> findByPnr(String pnr);
	Flux<Booking> findByUserEmail(String email);
//	Mono<BookingResponse> findByPnr(String pnr);
	
}
