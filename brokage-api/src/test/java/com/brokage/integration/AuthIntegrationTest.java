package com.brokage.integration;

import com.brokage.dto.request.LoginRequest;
import com.brokage.dto.request.RegisterRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginWithValidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("admin", "admin123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/login", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("token").asText()).isNotEmpty();
        assertThat(json.get("data").get("username").asText()).isEqualTo("admin");
        assertThat(json.get("data").get("role").asText()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void shouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/api/auth/login", entity, String.class);
            // If we get here, verify it's an error response
            assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        } catch (Exception e) {
            // RestTemplate throws exception for authentication failures in streaming mode
            // This is expected behavior for failed authentication
            assertThat(e.getMessage()).contains("authentication");
        }
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() throws Exception {
        String uniqueUsername = "testuser_" + System.currentTimeMillis();
        RegisterRequest request = new RegisterRequest(uniqueUsername, "password123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("token").asText()).isNotEmpty();
        assertThat(json.get("data").get("role").asText()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should reject registration with existing username")
    void shouldRejectDuplicateUsername() throws Exception {
        RegisterRequest request = new RegisterRequest("admin", "password123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/auth/register", entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should reject access without token")
    void shouldRejectAccessWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/orders?customerId=2", String.class);

        // Spring Security returns FORBIDDEN for unauthenticated requests to protected
        // endpoints
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
