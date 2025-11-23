package com.flightapp.dto;

import com.flightapp.entity.Gender;
import com.flightapp.entity.MealType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PassengerDTO {
    @NotBlank
    private String name;
    private Gender gender;
    @Min(0)
    private int age;
    @NotBlank
    private String seatNumber;
    private MealType mealType;
}
