package com.brokage.controller;

import com.brokage.dto.request.CreateOrderRequest;
import com.brokage.dto.response.OrderResponse;
import com.brokage.entity.Customer;
import com.brokage.enums.OrderSide;
import com.brokage.enums.OrderStatus;
import com.brokage.security.JwtAuthenticationFilter;
import com.brokage.security.JwtService;
import com.brokage.service.CustomerService;
import com.brokage.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should create order for admin")
    void createOrder_Admin_Success() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(2L)
                .assetName("GOOG")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10"))
                .price(new BigDecimal("100"))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("GOOG")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10"))
                .price(new BigDecimal("100"))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assetName").value("GOOG"));
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    @DisplayName("Should allow customer to create order for themselves")
    void createOrder_Customer_OwnOrder() throws Exception {
        Customer customer = Customer.builder()
                .id(2L)
                .username("customer1")
                .role("CUSTOMER")
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(2L)
                .assetName("GOOG")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10"))
                .price(new BigDecimal("100"))
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("GOOG")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10"))
                .price(new BigDecimal("100"))
                .status(OrderStatus.PENDING)
                .createDate(LocalDateTime.now())
                .build();

        when(customerService.findByUsername("customer1")).thenReturn(Optional.of(customer));
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    @DisplayName("Should deny customer creating order for other customer")
    void createOrder_Customer_OtherCustomer_Forbidden() throws Exception {
        Customer customer = Customer.builder()
                .id(2L)
                .username("customer1")
                .role("CUSTOMER")
                .build();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(999L)
                .assetName("GOOG")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("10"))
                .price(new BigDecimal("100"))
                .build();

        when(customerService.findByUsername("customer1")).thenReturn(Optional.of(customer));

        mockMvc.perform(post("/api/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should list orders for admin")
    void listOrders_Admin_Success() throws Exception {
        OrderResponse order1 = OrderResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("GOOG")
                .orderSide(OrderSide.BUY)
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.listOrders(eq(2L), any(), any()))
                .thenReturn(Arrays.asList(order1));

        mockMvc.perform(get("/api/orders")
                .param("customerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should cancel order for admin")
    void cancelOrder_Admin_Success() throws Exception {
        OrderResponse canceledOrder = OrderResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("GOOG")
                .status(OrderStatus.CANCELED)
                .build();

        when(orderService.getOrder(1L)).thenReturn(canceledOrder);
        when(orderService.cancelOrder(1L)).thenReturn(canceledOrder);

        mockMvc.perform(delete("/api/orders/1")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get order by id for admin")
    void getOrder_Admin_Success() throws Exception {
        OrderResponse order = OrderResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("GOOG")
                .status(OrderStatus.PENDING)
                .build();

        when(orderService.getOrder(1L)).thenReturn(order);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
