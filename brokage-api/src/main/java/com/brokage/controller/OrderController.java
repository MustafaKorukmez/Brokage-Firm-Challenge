package com.brokage.controller;

import com.brokage.dto.request.CreateOrderRequest;
import com.brokage.dto.response.ApiResponse;
import com.brokage.dto.response.OrderResponse;
import com.brokage.entity.Customer;
import com.brokage.service.CustomerService;
import com.brokage.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        // Check authorization - customers can only create orders for themselves
        if (!isAdminOrOwner(authentication, request.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Cannot create orders for other customers"));
        }

        OrderResponse order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listOrders(
            @RequestParam Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication authentication) {

        // Check authorization
        if (!isAdminOrOwner(authentication, customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Cannot view orders of other customers"));
        }

        List<OrderResponse> orders = orderService.listOrders(customerId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        // Get order to check ownership
        OrderResponse existingOrder = orderService.getOrder(orderId);

        if (!isAdminOrOwner(authentication, existingOrder.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Cannot cancel orders of other customers"));
        }

        OrderResponse order = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order canceled successfully", order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        OrderResponse order = orderService.getOrder(orderId);

        if (!isAdminOrOwner(authentication, order.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Cannot view orders of other customers"));
        }

        return ResponseEntity.ok(ApiResponse.success(order));
    }

    private boolean isAdminOrOwner(Authentication authentication, Long customerId) {
        // Admin can access everything
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // Check if customer owns the resource
        Customer customer = customerService.findByUsername(authentication.getName()).orElse(null);
        return customer != null && customer.getId().equals(customerId);
    }
}
