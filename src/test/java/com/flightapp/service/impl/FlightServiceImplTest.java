//package com.flightapp.service.impl;
//
//import com.flightapp.dto.request.AddInventoryRequest;
//import com.flightapp.dto.request.FlightSearchRequest;
//import com.flightapp.dto.response.FlightSearchResultDto;
//import com.flightapp.entity.Airline;
//import com.flightapp.entity.Flight;
//import com.flightapp.exception.BadRequestException;
//import com.flightapp.exception.NotFoundException;
//import com.flightapp.repository.AirlineRepository;
//import com.flightapp.repository.FlightRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//// using mockito with junit 5
//@ExtendWith(MockitoExtension.class)
//public class FlightServiceImplTest {
//
//    // faking repos here
//    @Mock
//    private AirlineRepository airlineRepository;
//
//    @Mock
//    private FlightRepository flightRepository;
//    // actual service under test (SUT)
//    @InjectMocks
//    private FlightServiceImpl flightService;
//    private Airline airline;
//    @BeforeEach
//    void init() {
//        // this is like some dummy airline present in db
//        airline = new Airline();
//        airline.setId(1L);
//        airline.setName("Test Airline");
//        airline.setCode("TA");
//        airline.setLogoUrl("http://logo.test");
//    }
//
//    //addInventory tests
//
//    @Test
//    void addInventory_shouldSaveFlight_andReturnId() {
//
//        // making a request that should be valid
//        AddInventoryRequest req = new AddInventoryRequest();
//        req.setAirlineId(1L);
//        req.setFlightNumber("TA-101");
//        req.setFromPlace("DELHI");
//        req.setToPlace("MUMBAI");
//        // departure before arrival (normal case)
//        LocalDateTime dep = LocalDateTime.now().plusDays(1);
//        LocalDateTime arr = dep.plusHours(2);
//        req.setDepartureDateTime(dep);
//        req.setArrivalDateTime(arr);
//        req.setPriceOneWay(new BigDecimal("5000"));
//        req.setPriceRoundTrip(new BigDecimal("9000"));
//        req.setTotalSeats(120);
//
//        // when airline is searched, we give our dummy airline
//        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
//
//        // fake saved flight object returned from repo
//        Flight saved = new Flight();
//        saved.setId(99L);
//        when(flightRepository.save(any(Flight.class))).thenReturn(saved);
//        // calling the service
//        Long newId = flightService.addInventory(req);
//        // id from saved flight should be returned
//        assertEquals(99L, newId);
//
//        // also check what exactly was passed to save()
//        ArgumentCaptor<Flight> captor = ArgumentCaptor.forClass(Flight.class);
//        verify(flightRepository).save(captor.capture());
//        Flight captured = captor.getValue();
//        // basic validations on created Flight
//        assertEquals("TA-101", captured.getFlightNumber());
//        assertEquals("DELHI", captured.getFromPlace());
//        assertEquals("MUMBAI", captured.getToPlace());
//        assertEquals(dep, captured.getDepartureDateTime());
//        assertEquals(arr, captured.getArrivalDateTime());
//        assertEquals(new BigDecimal("5000"), captured.getPriceOneWay());
//        assertEquals(new BigDecimal("9000"), captured.getPriceRoundTrip());
//        // available seats should start same as total seats
//        assertEquals(120, captured.getTotalSeats());
//        assertEquals(120, captured.getAvailableSeats());
//        assertEquals(airline, captured.getAirline());
//    }
//
//    @Test
//    void addInventory_shouldThrow_whenAirlineNotFound() {
//        // testing case where user passes airline id which doesn't exist
//        AddInventoryRequest req = new AddInventoryRequest();
//        req.setAirlineId(999L); // random id no one knows
//
//        // repo returns empty here, means no airline
//        when(airlineRepository.findById(999L)).thenReturn(Optional.empty());
//
//        // so we expect NotFoundException from service
//        assertThrows(NotFoundException.class,
//                () -> flightService.addInventory(req));
//
//        // save should never be called in this case
//        verify(flightRepository, never()).save(any());
//    }
//
//    @Test
//    void addInventory_shouldThrow_whenDepartureAfterOrEqualArrival() {
//
//        AddInventoryRequest req = new AddInventoryRequest();
//        req.setAirlineId(1L);
//        req.setFlightNumber("TA-102");
//
//        // we still need airline to be found properly
//        when(airlineRepository.findById(1L)).thenReturn(Optional.of(airline));
//
//        // case 1: departure == arrival  (weird, should fail)
//        LocalDateTime t = LocalDateTime.now().plusDays(1);
//        req.setDepartureDateTime(t);
//        req.setArrivalDateTime(t);
//
//        // expecting BadRequestException
//        assertThrows(BadRequestException.class,
//                () -> flightService.addInventory(req));
//
//        // case 2: departure is after arrival (even more wrong)
//        LocalDateTime dep2 = LocalDateTime.now().plusDays(2);
//        LocalDateTime arr2 = dep2.minusHours(3);
//        req.setDepartureDateTime(dep2);
//        req.setArrivalDateTime(arr2);
//
//        assertThrows(BadRequestException.class,
//                () -> flightService.addInventory(req));
//    }
//
//    //searchFlights tests
//
//    @Test
//    void searchFlights_shouldReturnMappedDtoList() {
//
//        FlightSearchRequest req = new FlightSearchRequest();
//        req.setFromPlace("DELHI");
//        req.setToPlace("MUMBAI");
//        LocalDate date = LocalDate.of(2025, 1, 1);
//        req.setDepartureDate(date);
//
//        // time range that method will internally calculate
//        LocalDateTime start = date.atStartOfDay();
//        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
//
//        // creating couple of flights that fall in that range
//        Flight f1 = new Flight();
//        f1.setId(1L);
//        f1.setAirline(airline);
//        f1.setFlightNumber("TA-111");
//        f1.setFromPlace("DELHI");
//        f1.setToPlace("MUMBAI");
//        f1.setDepartureDateTime(start.plusHours(2));
//        f1.setArrivalDateTime(start.plusHours(4));
//        f1.setPriceOneWay(new BigDecimal("4000"));
//        f1.setPriceRoundTrip(new BigDecimal("7500"));
//        f1.setAvailableSeats(50);
//
//        Flight f2 = new Flight();
//        f2.setId(2L);
//        f2.setAirline(airline);
//        f2.setFlightNumber("TA-222");
//        f2.setFromPlace("DELHI");
//        f2.setToPlace("MUMBAI");
//        f2.setDepartureDateTime(start.plusHours(5));
//        f2.setArrivalDateTime(start.plusHours(7));
//        f2.setPriceOneWay(new BigDecimal("6000"));
//        f2.setPriceRoundTrip(new BigDecimal("10000"));
//        f2.setAvailableSeats(10);
//
//        // mock repo call: for this from/to/date range, return these 2 flights
//        when(flightRepository.findByFromPlaceIgnoreCaseAndToPlaceIgnoreCaseAndDepartureDateTimeBetween(
//                "DELHI", "MUMBAI", start, end
//        )).thenReturn(Arrays.asList(f1, f2));
//
//        // call service
//        List<FlightSearchResultDto> result = flightService.searchFlights(req);
//
//        // should map to 2 DTOs
//        assertEquals(2, result.size());
//
//        FlightSearchResultDto d1 = result.get(0);
//        FlightSearchResultDto d2 = result.get(1);
//
//        // checking first dto mapping
//        assertEquals(1L, d1.getFlightId());
//        assertEquals("Test Airline", d1.getAirlineName());
//        assertEquals("http://logo.test", d1.getAirlineLogoUrl());
//        assertEquals("TA-111", d1.getFlightNumber());
//        assertEquals("DELHI", d1.getFromPlace());
//        assertEquals("MUMBAI", d1.getToPlace());
//        assertEquals(f1.getDepartureDateTime(), d1.getDepartureDateTime());
//        assertEquals(f1.getArrivalDateTime(), d1.getArrivalDateTime());
//        assertEquals(new BigDecimal("4000"), d1.getPriceOneWay());
//        assertEquals(new BigDecimal("7500"), d1.getPriceRoundTrip());
//        assertEquals(50, d1.getAvailableSeats());
//
//        // checking second dto mapping quickly
//        assertEquals(2L, d2.getFlightId());
//        assertEquals("TA-222", d2.getFlightNumber());
//        assertEquals(10, d2.getAvailableSeats());
//    }
//
//    @Test
//    void searchFlights_shouldReturnEmptyList_whenNoFlightsFound() {
//
//        FlightSearchRequest req = new FlightSearchRequest();
//        req.setFromPlace("SOMEWHERE");
//        req.setToPlace("NOWHERE");
//        LocalDate date = LocalDate.of(2025, 1, 2);
//        req.setDepartureDate(date);
//
//        LocalDateTime start = date.atStartOfDay();
//        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
//
//        // no flights in repo for this filter, return empty list
//        when(flightRepository.findByFromPlaceIgnoreCaseAndToPlaceIgnoreCaseAndDepartureDateTimeBetween(
//                "SOMEWHERE", "NOWHERE", start, end
//        )).thenReturn(Collections.emptyList());
//
//        // service should not fail, just give empty list back
//        List<FlightSearchResultDto> result = flightService.searchFlights(req);
//
//        assertNotNull(result);   // should not be null
//        assertTrue(result.isEmpty()); // and size 0
//    }
//
//    @Test
//    void searchFlights_shouldUseIgnoreCaseInRepoCall() {
//        // this test is just to be sure we pass the same from/to strings we get, 
//        // and repo method is the IgnoreCase one
//
//        FlightSearchRequest req = new FlightSearchRequest();
//        req.setFromPlace("delhi");
//        req.setToPlace("mumbai");
//        LocalDate date = LocalDate.of(2025, 1, 3);
//        req.setDepartureDate(date);
//
//        LocalDateTime start = date.atStartOfDay();
//        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
//
//        // we don't care about data here, just verifying method call
//        when(flightRepository.findByFromPlaceIgnoreCaseAndToPlaceIgnoreCaseAndDepartureDateTimeBetween(
//                anyString(), anyString(), any(), any()
//        )).thenReturn(Collections.emptyList());
//
//        flightService.searchFlights(req);
//        // verify we called repo with same strings we got in request
//        verify(flightRepository).findByFromPlaceIgnoreCaseAndToPlaceIgnoreCaseAndDepartureDateTimeBetween(
//                "delhi", "mumbai", start, end
//        );
//    }
//}
