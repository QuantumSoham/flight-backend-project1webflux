package com.flightapp.service.impl;

import com.flightapp.dto.BookingRequest;
import com.flightapp.dto.BookingResponse;
import com.flightapp.dto.PassengerDTO;
import com.flightapp.dto.TicketDTO;

import com.flightapp.entity.Airline;
import com.flightapp.entity.Booking;
import com.flightapp.entity.BookingStatus;
import com.flightapp.entity.Flight;
import com.flightapp.entity.Passenger;

import com.flightapp.exception.BadRequestException;
import com.flightapp.exception.ResourceNotFoundException;

import com.flightapp.repository.AirlineRepository;
import com.flightapp.repository.BookingRepository;
import com.flightapp.repository.FlightRepository;
import com.flightapp.repository.PassengerRepository;

import com.flightapp.service.BookingService;
import com.flightapp.util.PnrGenerator;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

	private final FlightRepository flightRepository;
	private final BookingRepository bookingRepository;
	private final PassengerRepository passengerRepository;
	private final AirlineRepository airlineRepository;
	private final ReactiveMongoTemplate template;
	private final PnrGenerator pnrGenerator;

	@Override
	public Mono<BookingResponse> bookTicket(String flightId, BookingRequest request) {
		if (request == null || request.getPassengers() == null
				|| request.getPassengers().size() != request.getNumberOfSeats()) {
			return Mono.error(new BadRequestException("numberOfSeats doesn't match passengers count"));
		}

		final int seatsToBook = request.getNumberOfSeats();

		// 1) Validate duplicate seats
		List<String> seatNumbers = request.getPassengers().stream().map(PassengerDTO::getSeatNumber)
				.filter(Objects::nonNull).collect(Collectors.toList());

		Set<String> seatSet = seatNumbers.stream().collect(Collectors.toSet());
		if (seatSet.size() != seatNumbers.size()) {
			return Mono.error(new BadRequestException("Duplicate seat numbers in request"));
		}

		// 2) Atomic seat reservation
		Query reserveQuery = Query.query(Criteria.where("_id").is(flightId).and("availableSeats").gte(seatsToBook)
				.and("bookedSeats").nin(seatNumbers));

		Update reserveUpdate = new Update().inc("availableSeats", -seatsToBook).push("bookedSeats")
				.each(seatNumbers.toArray(new String[0]));

		FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

		return template.findAndModify(reserveQuery, reserveUpdate, options, Flight.class)
				.switchIfEmpty(Mono
						.error(new BadRequestException("Insufficient seats or some requested seats are already taken")))
				.flatMap(updatedFlight -> {

					// PNR Logic

					// Build a seat signature: sort taken seats + join with "-"
					String seatSignature = request.getPassengers().stream().map(PassengerDTO::getSeatNumber)
							.filter(Objects::nonNull).sorted().collect(Collectors.joining("-"));

					// Generate PNR using flightNumber & seatSignature
					String pnr = pnrGenerator.generatePnr(updatedFlight.getFlightNumber(), seatSignature);
					final Booking booking = Booking.builder().pnr(pnr).flightId(flightId).userId(request.getUserId())
							.userName(request.getUserName()).userEmail(request.getUserEmail())
							.numberOfSeats(seatsToBook).bookingDateTime(Instant.now())
							.journeyDateTime(updatedFlight.getDepartureDateTime()).status(BookingStatus.BOOKED).build();

					// save booking
					return bookingRepository.save(booking)
							.flatMap(savedBooking -> savePassengers(savedBooking, request.getPassengers(), flightId)
									.then(Mono.just(savedBooking))
									.onErrorResume(passErr -> unreserveSeats(flightId, seatNumbers, seatsToBook)
											.then(bookingRepository.deleteById(savedBooking.getId()))
											.then(Mono.error(passErr))))
							.onErrorResume(saveErr -> unreserveSeats(flightId, seatNumbers, seatsToBook)
									.then(Mono.error(saveErr)))
							.map(b -> BookingResponse.builder().pnr(b.getPnr()).flightId(b.getFlightId())
									.userEmail(b.getUserEmail()).bookingId(b.getId())
									.numberOfSeats(b.getNumberOfSeats())
									.status(b.getStatus() != null ? b.getStatus().name() : null)
									.bookingDateTime(b.getBookingDateTime()).build());
				});
	}

	/**
	 * Remove reserved seats and increment availableSeats (used on rollback)
	 */
	private Mono<Void> unreserveSeats(String flightId, List<String> seatNumbers, int seatsToRestore) {
		if (seatNumbers == null || seatNumbers.isEmpty()) {
			// just increment seats if needed
			Query q = Query.query(Criteria.where("_id").is(flightId));
			Update inc = new Update().inc("availableSeats", seatsToRestore);
			return template.updateFirst(q, inc, Flight.class).then();
		}

		Query q = Query.query(Criteria.where("_id").is(flightId));
		Update undo = new Update().inc("availableSeats", seatsToRestore).pullAll("bookedSeats",
				seatNumbers.toArray(new String[0]));
		return template.updateFirst(q, undo, Flight.class).then();
	}

	private Mono<Void> savePassengers(Booking savedBooking, List<PassengerDTO> passengerDTOs, String flightId) {
		if (passengerDTOs == null || passengerDTOs.isEmpty()) {
			return Mono.empty();
		}
		return Flux.fromIterable(passengerDTOs)
				.map(dto -> Passenger.builder().bookingId(savedBooking.getId()).flightId(flightId).name(dto.getName())
						.age(dto.getAge()).gender(dto.getGender()).seatNumber(dto.getSeatNumber())
						.mealType(dto.getMealType()).build())
				.flatMap(passengerRepository::save).then().onErrorMap(e -> {
					if (e instanceof DuplicateKeyException) {
						return new BadRequestException("Seat already taken or duplicate seat");
					}
					return e;
				});
	}

	// if booking is not successful roll back the seats i have booked
	private Mono<Void> rollbackBookingAndSeats(Booking savedBooking, int seatsToRestore) {
		Query q = Query.query(Criteria.where("_id").is(savedBooking.getFlightId()));
		Update inc = new Update().inc("availableSeats", seatsToRestore);

		Mono<?> restore = template.updateFirst(q, inc, Flight.class).then();
		Mono<Void> deleteBooking = bookingRepository.deleteById(savedBooking.getId());

		return restore.then(deleteBooking)
				.onErrorResume(e -> bookingRepository.deleteById(savedBooking.getId()).then());
	}

	@Override
	public Mono<TicketDTO> getTicketByPnr(String pnr) {
		return bookingRepository.findByPnr(pnr)
				.switchIfEmpty(Mono.error(new ResourceNotFoundException("PNR not found"))).map(booking -> {
					System.out.println("PNR FOUND: " + booking.getPnr());

					// Return a very simple DTO with only PNR
					return TicketDTO.builder().pnr(booking.getPnr()).flightId(booking.getFlightId())
							.userEmail(booking.getUserEmail()).status(booking.getStatus())
							.flightNumber(booking.getFlightId()).journeyDateTime(booking.getJourneyDateTime())
							.userName(booking.getUserName()).build();
				});
	}

	@Override
	public Flux<BookingResponse> getHistoryByEmail(String email) {
		return ((BookingRepository) bookingRepository).findByUserEmail(email)
				.map(b -> BookingResponse.builder().pnr(b.getPnr()).flightId(b.getFlightId())
						.userEmail(b.getUserEmail()).numberOfSeats(b.getNumberOfSeats())
						.bookingDateTime(b.getBookingDateTime())
						.status(b.getStatus() != null ? b.getStatus().name() : null).build());
	}

	@Override
	public Mono<Void> cancelByPnr(String pnr) {
		return bookingRepository.findByPnr(pnr)
				.switchIfEmpty(Mono.error(new ResourceNotFoundException("Booking not found")))
				.flatMap((Booking booking) -> {
					if (Instant.now().isAfter(booking.getJourneyDateTime().minusSeconds(24 * 3600))) {
						return Mono.error(new BadRequestException("Cannot cancel within 24 hours of journey"));
					}

					booking.setStatus(BookingStatus.CANCELLED);
					final int seatsToRestore = booking.getNumberOfSeats();

					return bookingRepository.save(booking).flatMap((Booking b) -> {
						Query q = Query.query(Criteria.where("_id").is(b.getFlightId()));
						Update inc = new Update().inc("availableSeats", seatsToRestore);
						return template.updateFirst(q, inc, Flight.class).then();
					}).then();
				});
	}
}
