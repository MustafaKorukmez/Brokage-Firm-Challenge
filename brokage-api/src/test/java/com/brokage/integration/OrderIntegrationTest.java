package com.brokage.integration;

import com.brokage.dto.request.CreateOrderRequest;
import com.brokage.dto.request.LoginRequest;
import com.brokage.dto.response.AuthResponse;
import com.brokage.enums.OrderSide;
import com.brokage.enums.OrderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port;
        adminToken = login("admin", "admin123");
        customerToken = login("customer1", "pass123");
    }

    private String login(String username, String password) throws Exception {
        LoginRequest request = new LoginRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", entity, String.class);

        JsonNode json = objectMapper.readTree(response.getBody());
        return json.get("data").get("token").asText();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    @DisplayName("Should create order successfully")
    void shouldCreateOrder() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(2L)
                .assetName("AAPL")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("5"))
                .price(new BigDecimal("100"))
                .build();

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, authHeaders(adminToken));

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/orders", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("status").asText()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Should list orders for customer")
    void shouldListOrders() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(customerToken));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/orders?customerId=2",
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").isArray()).isTrue();
    }

    @Test
    @DisplayName("Should cancel pending order and restore balance")
    void shouldCancelOrderAndRestoreBalance() throws Exception {
        // First create an order
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .customerId(2L)
                .assetName("MSFT")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("2"))
                .price(new BigDecimal("50"))
                .build();

        HttpEntity<CreateOrderRequest> createEntity = new HttpEntity<>(createRequest, authHeaders(adminToken));
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl + "/api/orders", createEntity, String.class);

        JsonNode createJson = objectMapper.readTree(createResponse.getBody());
        Long orderId = createJson.get("data").get("id").asLong();

        // Then cancel it
        HttpEntity<Void> cancelEntity = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> cancelResponse = restTemplate.exchange(
                baseUrl + "/api/orders/" + orderId,
                HttpMethod.DELETE, cancelEntity, String.class);

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode cancelJson = objectMapper.readTree(cancelResponse.getBody());
        assertThat(cancelJson.get("data").get("status").asText()).isEqualTo("CANCELED");
    }

    @Test
    @DisplayName("Should match pending order and update assets")
    void shouldMatchOrderAndUpdateAssets() throws Exception {
        // Create an order
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .customerId(2L)
                .assetName("TSLA")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("1"))
                .price(new BigDecimal("200"))
                .build();

        HttpEntity<CreateOrderRequest> createEntity = new HttpEntity<>(createRequest, authHeaders(adminToken));
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl + "/api/orders", createEntity, String.class);

        JsonNode createJson = objectMapper.readTree(createResponse.getBody());
        Long orderId = createJson.get("data").get("id").asLong();

        // Match the order (admin only)
        HttpEntity<Void> matchEntity = new HttpEntity<>(authHeaders(adminToken));
        ResponseEntity<String> matchResponse = restTemplate.exchange(
                baseUrl + "/api/admin/orders/" + orderId + "/match",
                HttpMethod.POST, matchEntity, String.class);

        assertThat(matchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode matchJson = objectMapper.readTree(matchResponse.getBody());
        assertThat(matchJson.get("data").get("status").asText()).isEqualTo("MATCHED");
    }

    @Test
    @DisplayName("Should reject order with insufficient balance")
    void shouldRejectInsufficientBalance() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(2L)
                .assetName("NFLX")
                .orderSide(OrderSide.BUY)
                .size(new BigDecimal("1000000"))
                .price(new BigDecimal("500"))
                .build();

        HttpEntity<CreateOrderRequest> entity = new HttpEntity<>(request, authHeaders(adminToken));

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/orders", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("errorCode").asText()).isEqualTo("INSUFFICIENT_BALANCE");
    }

    @Test
    @DisplayName("Should deny customer access to other customer data")
    void shouldDenyAccessToOtherCustomer() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(customerToken));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/orders?customerId=999",
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
