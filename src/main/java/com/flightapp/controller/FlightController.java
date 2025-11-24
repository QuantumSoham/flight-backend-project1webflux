package com.flightapp.controller;

import com.flightapp.dto.*;
import com.flightapp.entity.Airline;
import com.flightapp.entity.Flight;
import com.flightapp.service.AirlineService;
import com.flightapp.service.BookingService;
import com.flightapp.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1.0/flight")
@RequiredArgsConstructor
public class FlightController {

	private final FlightService flightService;
	private final AirlineService airlineService;
	private final BookingService bookingService;

	@PostMapping("/airline")
	public Mono<Airline> addAirline(@Valid @RequestBody Airline airline) {
		return airlineService.addAirline(airline);
	}

	@PostMapping("/airline/inventory/{airlineCode}")
	public Mono<ResponseEntity<Object>> addInventory(@PathVariable("airlineCode") String airlineCode,
			@Valid @RequestBody CreateFlightRequest req) {

		return flightService.addInventory(airlineCode, req)
				.then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build())).onErrorResume(ex -> {
					if (ex instanceof ResponseStatusException rse) {
						return Mono.just(ResponseEntity.status(rse.getStatusCode()).build()); // <-- IMPORTANT FIX
					}
					ex.printStackTrace();
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()); // <-- IMPORTANT
																										// FIX
				});
	}

	@PostMapping("/search")
	public Flux<SearchResultDTO> search(@RequestBody SearchRequest request) {
		return flightService.searchFlights(request);
	}

	@PostMapping("/booking/{flightid}")
	public Mono<BookingResponse> bookTicket(@PathVariable("flightid") String flightId,
			@Valid @RequestBody BookingRequest request) {
		return bookingService.bookTicket(flightId, request);
	}

	@GetMapping("/ticket/{pnr}")
	public Mono<TicketDTO> getTicket(@PathVariable("pnr") String pnr) {
		return bookingService.getTicketByPnr(pnr);
	}

	@GetMapping("/booking/history/{emailId}")
	public Flux<BookingResponse> history(@PathVariable("emailId") String email) {
		return bookingService.getHistoryByEmail(email);
	}

	@DeleteMapping("/booking/cancel/{pnr}")
	public Mono<Void> cancel(@PathVariable("pnr") String pnr) {
		return bookingService.cancelByPnr(pnr);
	}
}
