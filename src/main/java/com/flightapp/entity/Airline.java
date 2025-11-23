package com.flightapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "airlines")
public class Airline {
    @Id
    private String id; // airline_id
    private String name;
    private String code; // unique
    private String logoUrl;
}
