package com.transcendance.backend.controller;


import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transcendance.backend.dto.RegisterRequestDto;
import com.transcendance.backend.services.RegisterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class RegisterController {
    
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RegisterController.class);

    private final RegisterService registerService;

    /**
     * Register a new user.
     *
     * @param registerRequestDto user's registration information
     * @param bindingResult validation result for the request
     * @return HTTP 201 if the user is created, or HTTP 400 if validation or registration fails
     */
    @PostMapping("/register")
    public ResponseEntity<?> processRegistration(
        @Valid @RequestBody RegisterRequestDto registerRequestDto,
        BindingResult bindingResult) {

    log.info("Processing registration for email: {}", registerRequestDto.getEmail());

    // Check whether validation errors were detected by @Valid.
    if (bindingResult.hasErrors()) {

        log.warn("Registration failed due to validation error: {}",
                bindingResult.getAllErrors().get(0).getDefaultMessage());

        // Return HTTP 400 with the validation errors.
        return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
    }

    try {
        // Delegate the registration logic to the service layer.
        registerService.registerUser(registerRequestDto);

        log.info("Registration successful for email: {}", registerRequestDto.getEmail());

        // Return HTTP 201 Created if the user was successfully created.
        return ResponseEntity.status(HttpStatus.CREATED).build();

    } catch (IllegalArgumentException ex) {

        // Business error (e.g. email already exists).
        log.warn("Registration failed: {}", ex.getMessage());

        // Return HTTP 400 with the business error message.
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
    }
}


