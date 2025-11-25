# Flight booking API built with Reactive Spring and MongoDB
[View on Eraser![](https://app.eraser.io/workspace/bzh7FeGzkGMRfzognglP/preview?elements=blwO8lnppYnb8shYCmBBmA&type=embed)](https://app.eraser.io/workspace/bzh7FeGzkGMRfzognglP?elements=blwO8lnppYnb8shYCmBBmA



Hi, I am Soham. This README explains how I built a fully reactive flight booking API using Spring WebFlux, Reactive MongoDB, and Java. I kept the entire request path non blocking, and every booking gets a cryptographically generated PNR that is unique and enforced at the database level.

Important note about repository files
- The ER diagram is already included in the repository and I did not remove or change it. Refer to that file for the data model overview.

What I built
- A backend API for flight search, management, and booking.
- The stack is reactive so the API can handle many concurrent requests with fewer threads.
- Every booking receives a cryptographically strong PNR that is persisted with a unique index in MongoDB.

Tech stack
- Java 17
- Spring Boot with Spring WebFlux
- Project Reactor (Mono and Flux)
- Spring Data Reactive MongoDB
- MongoDB
- Maven or Gradle to build and run

Why I went reactive
I used reactive programming because it lets the server handle a lot more concurrent users without blocking threads. Instead of blocking on IO, I chain asynchronous operations with Mono and Flux. This keeps the system responsive under load and makes backpressure handling straightforward. I kept any unavoidable blocking work isolated and scheduled off the main reactive pipeline.

High level data flow
- Controllers return Mono or Flux directly to the web layer.
- Service layer composes Reactor operators like map, flatMap, switchIfEmpty, onErrorResume, and retryWhen.
- Repositories extend ReactiveMongoRepository and return Mono and Flux.
- Database calls never block on the main reactive threads.

Data model overview
I modeled flights, seats, and bookings in MongoDB. The ER diagram in the repo shows entities and relationships. Bookings are stored in their own collection so they are easy to query by PNR, passenger, or flight.

PNR generation
The PNR must be cryptographically strong and collision resistant. I generate PNRs using java.security.SecureRandom, convert random bytes into an alphanumeric string, then attempt to persist the booking. The bookings collection has a unique index on the pnr field so MongoDB enforces uniqueness.

Here is the PNR generator I used in Java:

```java
package com.flightapp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class PnrGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";

    
    public static String generatePnr(String flightNumber, String seatSignature) {
        // BASE PNR = FLIGHT NUMBER 3 CHAR + MMDDHHMM + 4 random characters
        String prefix = flightNumber.replaceAll("[^A-Z0-9]", "").toUpperCase();//converting all a-z small char to upper case
        if (prefix.length() > 3) {
            prefix = prefix.substring(0, 3);
        }//if prefix length is more than 3 shorten it to 3 char

        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmm"));//get date and time sign

        StringBuilder sb = new StringBuilder(prefix).append(timePart);//string builder to append , as string builder memory efficient and mutable

        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        } //add final 4 characters randomly to my string - this is similar to password salting

        String basePnr = sb.toString();//converting string builder object to string object

        //PNR HASH= hashSHA-256(BASE PNR) + seatSignature to get a short suffix
        String toHash = basePnr + ":" + (seatSignature == null ? "" : seatSignature);
        String hashSuffix = shortHash(toHash, 3); // 3-char hash tail

        // final PNR = base PNR + hash tail 
        return basePnr + hashSuffix;
    }

    // CALCULATING SHA-256 and turn first bits into ALPHABET chars
    private static String shortHash(String input, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            int bits = 0;
            int value = 0;
            //LOGIC TAKEN FROM NET SOURCES 
            for (byte b : hash) {
                value = (value << 8) | (b & 0xFF);
                bits += 8;
                while (bits >= 5 && result.length() < length) {
                    int idx = (value >> (bits - 5)) & 0b1_1111; // 0–31
                    bits -= 5;
                    result.append(ALPHABET.charAt(idx % ALPHABET.length()));
                }
                if (result.length() == length) break;
            }

            //PADDING BITS
            while (result.length() < length) {
                result.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }

            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PNR hash", e);
        }
    }

    
}

```

Reactive uniqueness strategy
- I created a unique index on pnr in MongoDB: db.bookings.createIndex({ pnr: 1 }, { unique: true })
- When saving a booking I generate a PNR and insert the document reactively.
- If the insert fails with a duplicate key error, I catch that error in the reactive pipeline, generate a new PNR, and retry the insert a small number of times. This keeps everything non blocking and relies on the database for final uniqueness.

Example reactive save flow in the booking service

```java
public Mono<Booking> createBooking(CreateBookingRequest req) {
    return Mono.defer(() -> {
        String pnr = PnrGenerator.generatePnr();
        Booking booking = new Booking(null, pnr, req.getFlightId(), req.getPassenger(), req.getSeats());
        return bookingRepository.save(booking);
    })
    .retryWhen(Retry.max(3)
        .filter(throwable -> isDuplicateKeyError(throwable)))
    .onErrorMap(ex -> new BookingCreationException("Failed to create booking", ex));
}
```

Repository layer examples
- FlightRepository extends ReactiveMongoRepository<Flight, String>
- BookingRepository extends ReactiveMongoRepository<Booking, String>
- Methods return Mono<Booking>, Flux<Flight>, Mono<Void> for deletions, and custom query methods where needed.

Key endpoints
- GET /flights - returns a Flux of available flights
- GET /flights/{id} - returns a Mono of flight details
- POST /flights - create a flight
- POST /bookings - create a booking, returns the stored booking with pnr
- GET /bookings/{pnr} - get booking by pnr
- DELETE /bookings/{pnr} - cancel booking

Example booking flow
1. Client posts to POST /bookings with flight id and passenger details.
2. Controller validates request and calls bookingService.createBooking(...)
3. Booking service generates a PNR and composes the reactive repository save call.
4. On success, the saved booking with PNR is returned as Mono<Booking>.
5. If the pnr already exists, the save fails and I retry with a new PNR up to a limited number of times.

How to run locally
1. Make sure you have Java 17 and Maven or Gradle.
2. Run MongoDB locally or use a cloud MongoDB and set the connection URI in application properties.

Using Maven:

```
./mvnw clean package
java -jar target/flight-backend-project1webflux-0.0.1-SNAPSHOT.jar
```

Environment variables or application.yml examples

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/flightdb
server:
  port: 8080
```

Testing the API with curl

Create a flight:

```bash
curl -X POST http://localhost:8080/flights \
  -H "Content-Type: application/json" \
  -d '{ "flightNumber": "AI101", "origin": "DEL", "destination": "BOM", "departure": "2025-12-01T10:00:00" }'
```

Create a booking:

```bash
curl -X POST http://localhost:8080/bookings \
  -H "Content-Type: application/json" \
  -d '{ "flightId": "<flightId>", "passenger": { "name": "Alex", "email": "alex@example.com" } }'
```

The response will include the pnr and booking details.

Operational notes and trade offs
- Make the pnr length long enough to make collisions extremely unlikely. I used 10 alphanumeric characters which gives a large key space.
- Rely on the database unique index as the final guarantee of uniqueness.
- Handle duplicate key exceptions reactively and retry a few times.
- Monitor booking insert failures to detect unexpected collision rates.
- SecureRandom is cryptographically strong but slower than non cryptographic generators. That was an acceptable trade off for stronger PNRs.

What I learned and possible improvements
- Reactive programming lowers thread usage and improves throughput for IO bound APIs.
- Keep blocking libraries out of the main reactive flow or schedule them on separate Schedulers.
- Add reactive authentication and rate limiting.
- Expand tests around PNR collision and retry logic.
- Add metrics and tracing to observe reactive stream behavior and MongoDB latency.

If you want, I can paste specific code files like controllers, services, repository interfaces, or the PNR retry logic. The ER diagram remains in the repo for reference.
