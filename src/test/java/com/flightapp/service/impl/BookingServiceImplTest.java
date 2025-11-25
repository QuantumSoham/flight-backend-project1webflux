//package com.flightapp.service.impl;
//
//import com.flightapp.dto.BookFlightRequest;
//import com.flightapp.dto.request.PassengerRequest;
//import com.flightapp.dto.response.BookingHistoryItemDto;
//import com.flightapp.dto.response.TicketResponse;
//import com.flightapp.entity.Airline;
//import com.flightapp.entity.Booking;
//import com.flightapp.entity.Flight;
//import com.flightapp.entity.Passenger;
//import com.flightapp.exception.SeatNotAvailableException;
//import com.flightapp.repository.BookingRepository;
//import com.flightapp.repository.FlightRepository;
//import com.flightapp.repository.PassengerRepository;
//import com.flightapp.repository.UserAccountRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//// just telling junit to use mockito with this test class
//@ExtendWith(MockitoExtension.class)
//public class BookingServiceImplTest {
//
//	// fake repos, so we don't touch real database in tests
//	@Mock
//	private FlightRepository flightRepository;
//	@Mock
//	private BookingRepository bookingRepository;
//	@Mock
//	private PassengerRepository passengerRepository;
//	@Mock
//	private UserAccountRepository userAccountRepository;
//
//	// actual service we are testing, mock deps will be injected here
//	@InjectMocks
//	private BookingServiceImpl bookingService;
//	// common flight object reused in multiple test cases
//	private Flight flight;
//
//	@BeforeEach
//	void setup() {
//		// basic airline data, not super important for logic but needed for flight
//		Airline airline = new Airline();
//		airline.setId(1L);
//		airline.setName("Air India");
//		airline.setCode("AI");
//
//		// dummy flight present in "db"
//		flight = new Flight();
//		flight.setId(1L);
//		flight.setAirline(airline);
//		flight.setFlightNumber("AI-202");
//		flight.setFromPlace("DELHI");
//		flight.setToPlace("MUMBAI");
//		flight.setDepartureDateTime(LocalDateTime.now().plusDays(2)); // flight after 2 days
//		flight.setAvailableSeats(10); // initially 10 seats available
//	}
//
//	//bookFlight tests
//	@Test
//	void bookFlight_shouldThrow_whenNotEnoughSeats() {
//		// here i am forcing a scenario only 1 seat left
//		flight.setAvailableSeats(1);
//
//		// whenever service calls flightRepository.findById(1L), return this same flight
//		when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
//
//		// creating simple booking request with 2 seats
//		BookFlightRequest req = new BookFlightRequest();
//		req.setUserName("Test User");
//		req.setUserEmail("test@example.com");
//		req.setNumberOfSeats(2); // user is asking for 2 seats
//
//		// passenger list has 2 passengers to match above seat count
//		PassengerRequest p1 = new PassengerRequest();
//		p1.setName("A");
//		PassengerRequest p2 = new PassengerRequest();
//		p2.setName("B");
//		req.setPassengers(Arrays.asList(p1, p2));
//
//		// idea: since we have only 1 seat but user wants 2, service should throw
//		// SeatNotAvailableException
//		assertThrows(SeatNotAvailableException.class, () -> bookingService.bookFlight(1L, req));
//	}
//
//	@Test
//	void bookFlight_shouldCreateBooking_andDecreaseSeats() {
//		// here we are testing the happy path: enough seats, booking should succeed
//		when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
//
//		// booking request with 2 seats and 2 passengers
//		BookFlightRequest req = new BookFlightRequest();
//		req.setUserName("Test User");
//		req.setUserEmail("test@example.com");
//		req.setNumberOfSeats(2);
//
//		// passenger 1 details
//		PassengerRequest p1 = new PassengerRequest();
//		p1.setName("Alice");
//		p1.setGender(Passenger.Gender.FEMALE);
//		p1.setAge(25);
//		p1.setSeatNumber("13A");
//		p1.setMealType(Passenger.MealType.VEG);
//
//		// passenger 2 details
//		PassengerRequest p2 = new PassengerRequest();
//		p2.setName("Bob");
//		p2.setGender(Passenger.Gender.MALE);
//		p2.setAge(27);
//		p2.setSeatNumber("13B");
//		p2.setMealType(Passenger.MealType.NON_VEG);
//
//		req.setPassengers(Arrays.asList(p1, p2));
//
//		// this booking object is what we pretend db will return after save()
//		Booking saved = new Booking();
//		saved.setId(100L);
//		saved.setFlight(flight);
//		saved.setPnr("AI2512ABCD");
//		saved.setNumberOfSeats(2);
//		saved.setUserName("Test User");
//		saved.setUserEmail("test@example.com");
//		saved.setJourneyDateTime(flight.getDepartureDateTime());
//		saved.setStatus(Booking.Status.BOOKED);
//
//		// mock behavior of bookingRepository.save() -> always give us our "saved"
//		// booking
//		when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
//
//		// call the actual service method
//		TicketResponse resp = bookingService.bookFlight(1L, req);
//
//		// now we check if response looks like what we expect
//		assertEquals("AI2512ABCD", resp.getPnr()); // pnr should match mocked booking
//		assertEquals("Test User", resp.getUserName()); // username from request
//		assertEquals(2, resp.getNumberOfSeats()); // same seat count as requested
//		assertEquals("BOOKED", resp.getStatus()); // status should be BOOKED
//
//		// initial seats = 10, booked 2, so remaining should be 8
//		assertEquals(8, flight.getAvailableSeats());
//
//		// also making sure these repository methods were actually called
//		verify(flightRepository).save(flight);
//		verify(passengerRepository).saveAll(anyList());
//	}
//
//	// getTicketByPnr tests 
//
//	@Test
//	void getTicketByPnr_shouldReturnTicket_whenPnrExists() {
//		// here we assume booking already exists with this PNR
//		Booking booking = new Booking();
//		booking.setPnr("AI22XX88");
//		booking.setUserName("Soham");
//		booking.setUserEmail("soham@example.com");
//		booking.setNumberOfSeats(1);
//		booking.setStatus(Booking.Status.BOOKED);
//		booking.setFlight(flight); // linking the same flight created in setup()
//
//		// repo will return our dummy booking when searched by this pnr
//		when(bookingRepository.findByPnr("AI22XX88")).thenReturn(Optional.of(booking));
//
//		// service call
//		TicketResponse resp = bookingService.getTicketByPnr("AI22XX88");
//
//		// verifying data mapping from entity -> dto
//		assertEquals("AI22XX88", resp.getPnr());
//		assertEquals("Soham", resp.getUserName());
//		assertEquals("soham@example.com", resp.getUserEmail());
//
//		// just a casual print to see object in console while running tests
//		System.out.println("ticket response = " + resp);
//	}
//
//	@Test
//	void getTicketByPnr_shouldThrow_whenPnrNotFound() {
//		// in this test, repository returns empty -> means invalid or unknown pnr
//		when(bookingRepository.findByPnr("XXXXX")).thenReturn(Optional.empty());
//
//		// expectation: service should throw some exception when pnr is not found
//		// right now using RuntimeException,i can replace with my own
//		// NotFoundException later, will implement it later
//		assertThrows(RuntimeException.class, () -> bookingService.getTicketByPnr("XXXXX"));
//	}
//
//	//cancelBooking tests
//
//	@Test
//	void cancelBooking_shouldThrow_whenTooCloseToJourney() {
//		// scenario: user is trying to cancel when flight is less than 24 hours away
//		Booking booking = new Booking();
//		booking.setPnr("AI123XYZ");
//		booking.setJourneyDateTime(LocalDateTime.now().plusHours(10)); // 10 hours left
//		booking.setStatus(Booking.Status.BOOKED);
//		booking.setFlight(flight);
//		booking.setNumberOfSeats(2);
//
//		when(bookingRepository.findByPnr("AI123XYZ")).thenReturn(Optional.of(booking));
//
//		// per rule, cancellation not allowed so close to departure -> should throw
//		assertThrows(RuntimeException.class, () -> bookingService.cancelBooking("AI123XYZ"));
//	}
//
//	//getBookingHistory tests
//
//	@Test
//	void getBookingHistory_shouldReturnList() {
//		// making two bookings for same user, with different booking times
//		Booking one = new Booking();
//		one.setPnr("PNR1");
//		one.setUserEmail("test@x.com");
//		one.setBookingDateTime(LocalDateTime.now()); // latest booking
//
//		Booking two = new Booking();
//		two.setPnr("PNR2");
//		two.setUserEmail("test@x.com");
//		two.setBookingDateTime(LocalDateTime.now().minusDays(1)); // older booking
//
//		// repo already returns list sorted in desc by bookingDateTime
//		when(bookingRepository.findByUserEmailOrderByBookingDateTimeDesc("test@x.com"))
//				.thenReturn(Arrays.asList(one, two));
//
//		// calling service to get booking history
//		List<BookingHistoryItemDto> list = bookingService.getBookingHistory("test@x.com");
//
//		// basic sanity check: two items should be present
//		assertEquals(2, list.size());
//
//		// since it's desc, PNR1 (latest) should come first
//		assertEquals("PNR1", list.get(0).getPnr());
//		assertEquals("PNR2", list.get(1).getPnr());
//	}
//
//	@Test
//	void getBookingHistory_shouldReturnEmptyList_whenNoBookings() 
//	{
//		// here we test edge case: user has no bookings at all
//		when(bookingRepository.findByUserEmailOrderByBookingDateTimeDesc("nobody@example.com"))
//				.thenReturn(Collections.emptyList());
//
//		List<BookingHistoryItemDto> list = bookingService.getBookingHistory("nobody@example.com");
//
//		// service should just return empty list, not null
//		assertTrue(list.isEmpty());
//	}
//}
