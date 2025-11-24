package com.flightapp.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.flightapp.entity.UserAccount;

import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<UserAccount, String> {

	Mono<UserAccount> findByEmail(String email);
}
