package com.flightapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "passengers")
@CompoundIndex(def = "{'flightId': 1, 'seatNumber': 1}", name = "flight_seat_idx", unique = true)
public class Passenger {
    @Id
    private String id; // passenger_id
    private String bookingId;
    private String flightId;
    private String name;
    private Gender gender;
    private Integer age;
    private String seatNumber;
    private MealType mealType;
}
