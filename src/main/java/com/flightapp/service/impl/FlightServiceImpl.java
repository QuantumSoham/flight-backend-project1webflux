package com.flightapp.service.impl;

import com.flightapp.dto.CreateFlightRequest;
import com.flightapp.dto.SearchRequest;
import com.flightapp.dto.SearchResultDTO;
import com.flightapp.entity.Flight;
import com.flightapp.repository.AirlineRepository;
import com.flightapp.repository.FlightRepository;
import com.flightapp.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {

	private final FlightRepository flightRepository;
	private final AirlineRepository airlineRepository;
	private final ReactiveMongoTemplate template;

	@Override
	public Flux<SearchResultDTO> searchFlights(SearchRequest request) {
		// Documenting the aggregation pipeline for my convinience:
		// Converting LocalDate into an instant range for that whole day.
		// Mongo stores date as Instant, so to match by date only,
		// we generate "start of the day" and "start of next day".
		ZoneId zone = ZoneId.of("Asia/Kolkata");
		Instant start = request.getDepartureDate().atStartOfDay(zone).toInstant();
		Instant end = request.getDepartureDate().plusDays(1).atStartOfDay(zone).toInstant();

		// This match stage filters the flights early.
		// So at this point we only allow documents where:
		// - fromPlace == request.fromPlace
		// - toPlace == request.toPlace
		// - departureDateTime is inside the date range we just calculated.
		MatchOperation match = Aggregation.match(Criteria.where("fromPlace").is(request.getFromPlace()).and("toPlace")
				.is(request.getToPlace()).and("departureDateTime").gte(start).lt(end));

		// Now I join the airline details into each flight document.
		// Mongo doesn't have JOINs, so this is the lookup stage.
		// from() = source collection
		// localField() = field in flights
		// foreignField() = id in airlines
		// as() = new array field added to the pipeline output
		LookupOperation lookupAirline = LookupOperation.newLookup().from("airlines").localField("airlineId")
				.foreignField("_id").as("airline");

		// Unwinding because lookup produces an array.
		// But each flight belongs to exactly one airline,
		// so converting the array into a single object makes the final projection
		// cleaner.
		UnwindOperation unwindAirline = Aggregation.unwind("airline");

		// Final projection.
		// This stage decides exactly what the output looks like.
		// I pick out only the fields that my SearchResultDTO needs.
		// Anything not projected here doesn't reach the response.
		ProjectionOperation project = Aggregation
				.project("flightNumber", "departureDateTime", "arrivalDateTime", "priceOneWay", "priceRoundTrip",
						"availableSeats")
				.and("airline.name").as("airlineName").and("airline.logoUrl").as("airlineLogoUrl").and("_id")
				.as("flightId");

		// Building the entire aggregation pipeline step-by-step.
		Aggregation agg = Aggregation.newAggregation(match, lookupAirline, unwindAirline, project);

		// Running the entire pipeline on the "flights" collection
		// and mapping the final shaped output to SearchResultDTO.
		return template.aggregate(agg, "flights", SearchResultDTO.class);
	}

	@Override
	public reactor.core.publisher.Mono<Void> addInventory(String airlineCode, CreateFlightRequest req) {

		// First I check if an airline with the given code even exists.
		// If not found -> throw NOT_FOUND.
		return airlineRepository.findByCode(airlineCode)
				.switchIfEmpty(
						reactor.core.publisher.Mono.error(new org.springframework.web.server.ResponseStatusException(
								org.springframework.http.HttpStatus.NOT_FOUND,
								"Airline with code '" + airlineCode + "' not found")))
				.flatMap(airline -> {

					// Constructing the flight document from the request.
					// availableSeats defaults to totalSeats if not provided.
					Flight flight = Flight.builder().airlineId(airline.getId()).flightNumber(req.getFlightNumber())
							.fromPlace(req.getFromPlace()).toPlace(req.getToPlace())
							.departureDateTime(req.getDepartureDateTime()).arrivalDateTime(req.getArrivalDateTime())
							.priceOneWay(req.getPriceOneWay()).priceRoundTrip(req.getPriceRoundTrip())
							.totalSeats(req.getTotalSeats())
							.availableSeats(
									req.getAvailableSeats() != null ? req.getAvailableSeats() : req.getTotalSeats())
							.build();

					// Saving into Mongo.
					return flightRepository.save(flight);
				}).then(); // returning Mono<Void>
	}

}
