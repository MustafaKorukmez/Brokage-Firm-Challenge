package com.brokage.controller;

import com.brokage.dto.response.ApiResponse;
import com.brokage.dto.response.OrderResponse;
import com.brokage.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final OrderService orderService;

    @PostMapping("/orders/{orderId}/match")
    public ResponseEntity<ApiResponse<OrderResponse>> matchOrder(@PathVariable Long orderId) {
        OrderResponse order = orderService.matchOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Order matched successfully", order));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> listAllOrders() {
        List<OrderResponse> orders = orderService.listAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}
