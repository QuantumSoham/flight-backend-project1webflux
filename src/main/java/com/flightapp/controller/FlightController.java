package com.flightapp.controller;

//documenting my code for future reference
import com.flightapp.dto.*;
import com.flightapp.entity.Airline;
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

//Tells Spring this class handles REST APIs and returns JSON directly.
@RestController
//Base path for all APIs in this controller.
@RequestMapping("/api/flight")
//Auto-generates constructor for final fields. Enables constructor injection cleanly.
@RequiredArgsConstructor

public class FlightController {

	// These services are auto-injected through the generated constructor.
	// i did not use @Autowired
	private final FlightService flightService;
	private final AirlineService airlineService;
	private final BookingService bookingService;

	// @PostMapping Handles POST requests at /airline
	// @Valid Validates the @RequestBody fields using JSR-380 (javax validation)
	// @RequestBody Converts JSON body  Airline object
	@PostMapping("/airline")
	public Mono<Airline> addAirline(@Valid @RequestBody Airline airline) {
		return airlineService.addAirline(airline);
	}

	// POST /airline/inventory/{airlineCode}
	// @PathVariable Extracts dynamic segment from the URL
	@PostMapping("/airline/inventory/{airlineCode}")
	public Mono<ResponseEntity<Object>> addInventory(@PathVariable("airlineCode") String airlineCode,
			@Valid @RequestBody CreateFlightRequest req) {
		// .then() returns void response after success
		return flightService.addInventory(airlineCode, req)
				.then(Mono.just(ResponseEntity.status(HttpStatus.CREATED).build()))
				// onErrorResume is reactive try/catch for exceptions
				.onErrorResume(ex -> {
					// If the error is ResponseStatusException return that status code
					if (ex instanceof ResponseStatusException rse) {
						return Mono.just(ResponseEntity.status(rse.getStatusCode()).build()); // <-- IMPORTANT FIX
					}
					// Otherwise send 500
					// Mono sends a single response in reactive programming
					ex.printStackTrace();
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()); // <-- IMPORTANT
																										// FIX
				});
	}

	// POST /search
	// Flux<T> means a Reactive stream of multiple items
	// I put No @Valid because it's not necessary for simple DTOs
	@PostMapping("/search")
	public Flux<SearchResultDTO> search(@RequestBody SearchRequest request) {
		return flightService.searchFlights(request);
	}

	// POST /booking/{flightid}
	@PostMapping("/booking/{flightid}")
	public Mono<BookingResponse> bookTicket(@PathVariable("flightid") String flightId,
			@Valid @RequestBody BookingRequest request) {
		// Takes path variable + body
		return bookingService.bookTicket(flightId, request);
	}

	// GET /ticket/{pnr}
	// Mono<T> → returning a single item asynchronously
	@GetMapping("/ticket/{pnr}")
	public Mono<TicketDTO> getTicket(@PathVariable("pnr") String pnr) {
		return bookingService.getTicketByPnr(pnr);
	}

	// GET history by email
	// Flux (multiple values) because i might return many bookings
	@GetMapping("/booking/history/{emailId}")
	public Flux<BookingResponse> history(@PathVariable("emailId") String email) {
		return bookingService.getHistoryByEmail(email);
	}

	// DELETE cancel a booking
	// Mono<Void> means completes with no content
	// in case error in deletion , message shown by service layer
	@DeleteMapping("/booking/cancel/{pnr}")
	public Mono<Void> cancel(@PathVariable("pnr") String pnr) {
		return bookingService.cancelByPnr(pnr);
	}
}
