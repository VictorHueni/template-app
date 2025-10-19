package com.example.demo;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin(origins = "*") // simplify for test
@RestController
public class HelloController {
    @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","ok"); }
    @GetMapping("/api/hello") public Map<String,String> hello(){ return Map.of("message","hello from spring"); }
}