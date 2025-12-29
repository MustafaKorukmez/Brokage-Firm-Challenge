package com.brokage.integration;

import com.brokage.dto.request.LoginRequest;
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
class AssetIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port;
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
    @DisplayName("Should list customer assets")
    void shouldListAssets() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(customerToken));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/assets?customerId=2",
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode json = objectMapper.readTree(response.getBody());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").isArray()).isTrue();
        assertThat(json.get("data").size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should include TRY and GOOG for sample customer")
    void shouldHaveInitialAssets() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(customerToken));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/assets?customerId=2",
                HttpMethod.GET, entity, String.class);

        JsonNode json = objectMapper.readTree(response.getBody());
        JsonNode assets = json.get("data");

        boolean hasTry = false;
        boolean hasGoog = false;
        for (JsonNode asset : assets) {
            String name = asset.get("assetName").asText();
            if ("TRY".equals(name))
                hasTry = true;
            if ("GOOG".equals(name))
                hasGoog = true;
        }

        assertThat(hasTry).isTrue();
        assertThat(hasGoog).isTrue();
    }

    @Test
    @DisplayName("Should deny access to other customer assets")
    void shouldDenyAccessToOtherCustomerAssets() throws Exception {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(customerToken));

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/assets?customerId=999",
                HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
