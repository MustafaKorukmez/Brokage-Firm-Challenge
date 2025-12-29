package com.brokage.controller;

import com.brokage.dto.response.AssetResponse;
import com.brokage.entity.Customer;
import com.brokage.security.JwtAuthenticationFilter;
import com.brokage.security.JwtService;
import com.brokage.service.AssetService;
import com.brokage.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AssetController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssetService assetService;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should list assets for admin")
    void listAssets_Admin_Success() throws Exception {
        AssetResponse tryAsset = AssetResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("TRY")
                .size(new BigDecimal("10000"))
                .usableSize(new BigDecimal("10000"))
                .build();

        AssetResponse googAsset = AssetResponse.builder()
                .id(2L)
                .customerId(2L)
                .assetName("GOOG")
                .size(new BigDecimal("50"))
                .usableSize(new BigDecimal("50"))
                .build();

        when(assetService.getAssetsByCustomerId(2L))
                .thenReturn(Arrays.asList(tryAsset, googAsset));

        mockMvc.perform(get("/api/assets")
                .param("customerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    @DisplayName("Should allow customer to list own assets")
    void listAssets_Customer_OwnAssets() throws Exception {
        Customer customer = Customer.builder()
                .id(2L)
                .username("customer1")
                .role("CUSTOMER")
                .build();

        AssetResponse tryAsset = AssetResponse.builder()
                .id(1L)
                .customerId(2L)
                .assetName("TRY")
                .size(new BigDecimal("10000"))
                .usableSize(new BigDecimal("10000"))
                .build();

        when(customerService.findByUsername("customer1")).thenReturn(Optional.of(customer));
        when(assetService.getAssetsByCustomerId(2L)).thenReturn(Arrays.asList(tryAsset));

        mockMvc.perform(get("/api/assets")
                .param("customerId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    @DisplayName("Should deny customer listing other customer assets")
    void listAssets_Customer_OtherAssets_Forbidden() throws Exception {
        Customer customer = Customer.builder()
                .id(2L)
                .username("customer1")
                .role("CUSTOMER")
                .build();

        when(customerService.findByUsername("customer1")).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/api/assets")
                .param("customerId", "999"))
                .andExpect(status().isForbidden());
    }
}
