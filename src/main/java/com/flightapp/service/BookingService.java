package com.flightapp.service;

import com.flightapp.dto.BookingRequest;
import com.flightapp.dto.BookingResponse;
import com.flightapp.dto.TicketDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BookingService {
	//book a ticket
    Mono<BookingResponse> bookTicket(String flightId, BookingRequest request);
    //get a particular ticket by pnr
    Mono<TicketDTO> getTicketByPnr(String pnr);
    //get all booking by a email
    Flux<BookingResponse> getHistoryByEmail(String email);
    //cancel a ticket by pnr
    Mono<Void> cancelByPnr(String pnr);
}
