package com.example.demo.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebApiConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Automatically prefix all classes in the 'api' package with /api/v1
        configurer.addPathPrefix("/api/v1",
                c -> c.getPackageName().startsWith("com.example.demo.api")
                        && c.isAnnotationPresent(RestController.class));
    }
}