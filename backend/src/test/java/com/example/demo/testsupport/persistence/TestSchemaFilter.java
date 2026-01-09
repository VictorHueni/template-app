package com.example.demo.testsupport.persistence;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

public class TestSchemaFilter extends OncePerRequestFilter {

    public static final String TEST_SCHEMA_HEADER = "X-Test-Schema";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String schema = request.getHeader(TEST_SCHEMA_HEADER);
        try {
            if (schema != null && !schema.isBlank()) {
                SchemaContext.setSchema(schema);
            }
            filterChain.doFilter(request, response);
        }
        finally {
            SchemaContext.clear();
        }
    }
}
