package com.brokage.controller;

import com.brokage.dto.response.OrderResponse;
import com.brokage.enums.OrderSide;
import com.brokage.enums.OrderStatus;
import com.brokage.security.JwtAuthenticationFilter;
import com.brokage.security.JwtService;
import com.brokage.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private OrderService orderService;

        @MockBean
        private JwtService jwtService;

        @MockBean
        private UserDetailsService userDetailsService;

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should match order for admin")
        void matchOrder_Admin_Success() throws Exception {
                OrderResponse matchedOrder = OrderResponse.builder()
                                .id(1L)
                                .customerId(2L)
                                .assetName("GOOG")
                                .orderSide(OrderSide.BUY)
                                .size(new BigDecimal("10"))
                                .price(new BigDecimal("100"))
                                .status(OrderStatus.MATCHED)
                                .createDate(LocalDateTime.now())
                                .build();

                when(orderService.matchOrder(1L)).thenReturn(matchedOrder);

                mockMvc.perform(post("/api/admin/orders/1/match"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.status").value("MATCHED"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should list all orders for admin")
        void listAllOrders_Admin_Success() throws Exception {
                OrderResponse order1 = OrderResponse.builder()
                                .id(1L)
                                .customerId(2L)
                                .assetName("GOOG")
                                .status(OrderStatus.PENDING)
                                .build();

                OrderResponse order2 = OrderResponse.builder()
                                .id(2L)
                                .customerId(3L)
                                .assetName("AAPL")
                                .status(OrderStatus.MATCHED)
                                .build();

                when(orderService.listAllOrders()).thenReturn(Arrays.asList(order1, order2));

                mockMvc.perform(get("/api/admin/orders"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2));
        }
}
