package com.brokage.controller;

import com.brokage.dto.response.ApiResponse;
import com.brokage.dto.response.AssetResponse;
import com.brokage.entity.Customer;
import com.brokage.service.AssetService;
import com.brokage.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetResponse>>> listAssets(
            @RequestParam Long customerId,
            Authentication authentication) {

        if (!isAdminOrOwner(authentication, customerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Cannot view assets of other customers"));
        }

        List<AssetResponse> assets = assetService.getAssetsByCustomerId(customerId);
        return ResponseEntity.ok(ApiResponse.success(assets));
    }

    private boolean isAdminOrOwner(Authentication authentication, Long customerId) {
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        Customer customer = customerService.findByUsername(authentication.getName()).orElse(null);
        return customer != null && customer.getId().equals(customerId);
    }
}
