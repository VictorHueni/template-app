package com.example.demo.greeting.application;

public record CreateGreetingCommand(String name) {
    public CreateGreetingCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}