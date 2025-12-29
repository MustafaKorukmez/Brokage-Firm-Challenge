package com.brokage.controller;

import com.brokage.dto.request.LoginRequest;
import com.brokage.dto.request.RegisterRequest;
import com.brokage.dto.response.ApiResponse;
import com.brokage.dto.response.AuthResponse;
import com.brokage.entity.Asset;
import com.brokage.entity.Customer;
import com.brokage.repository.AssetRepository;
import com.brokage.repository.CustomerRepository;
import com.brokage.security.JwtService;
import com.brokage.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerService customerService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            UserDetails userDetails = customerService.loadUserByUsername(request.getUsername());
            Customer customer = customerService.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            String token = jwtService.generateToken(userDetails);

            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .username(customer.getUsername())
                    .role(customer.getRole())
                    .customerId(customer.getId())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Check if username exists
            if (customerRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Username already exists"));
            }

            // Create customer with CUSTOMER role
            Customer customer = customerService.createCustomer(
                    request.getUsername(),
                    request.getPassword(),
                    "CUSTOMER");

            // Give new customer initial TRY balance
            Asset tryAsset = Asset.builder()
                    .customerId(customer.getId())
                    .assetName("TRY")
                    .size(new BigDecimal("10000"))
                    .usableSize(new BigDecimal("10000"))
                    .build();
            assetRepository.save(tryAsset);

            // Generate token
            UserDetails userDetails = customerService.loadUserByUsername(customer.getUsername());
            String token = jwtService.generateToken(userDetails);

            AuthResponse authResponse = AuthResponse.builder()
                    .token(token)
                    .username(customer.getUsername())
                    .role(customer.getRole())
                    .customerId(customer.getId())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Registration successful", authResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
