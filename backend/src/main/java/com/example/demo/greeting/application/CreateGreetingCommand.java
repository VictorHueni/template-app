package com.example.demo.greeting.application;

public record CreateGreetingCommand(String message, String recipient) {
    public CreateGreetingCommand {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}