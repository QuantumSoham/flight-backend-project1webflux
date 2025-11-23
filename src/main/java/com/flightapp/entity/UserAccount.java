package com.flightapp.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class UserAccount {
	@Id
	private String id; // user_id
	private String name;
	private String email; // unique
	private String passwordHash;
}
