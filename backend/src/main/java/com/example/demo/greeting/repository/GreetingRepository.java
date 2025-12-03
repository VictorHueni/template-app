package com.example.demo.greeting.repository;

import com.example.demo.greeting.model.Greeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GreetingRepository extends JpaRepository<Greeting, UUID> {
}
