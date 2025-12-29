package com.brokage.config;

import com.brokage.entity.Asset;
import com.brokage.entity.Customer;
import com.brokage.repository.AssetRepository;
import com.brokage.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final AssetRepository assetRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Create admin user
        if (!customerRepository.existsByUsername("admin")) {
            Customer admin = Customer.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .build();
            customerRepository.save(admin);
            log.info("Admin user created: admin/admin123");
        }

        // Create sample customer
        if (!customerRepository.existsByUsername("customer1")) {
            Customer customer = Customer.builder()
                    .username("customer1")
                    .password(passwordEncoder.encode("pass123"))
                    .role("CUSTOMER")
                    .build();
            Customer savedCustomer = customerRepository.save(customer);
            log.info("Sample customer created: customer1/pass123 with ID: {}", savedCustomer.getId());

            // Add TRY balance for sample customer
            Asset tryAsset = Asset.builder()
                    .customerId(savedCustomer.getId())
                    .assetName("TRY")
                    .size(new BigDecimal("100000"))
                    .usableSize(new BigDecimal("100000"))
                    .build();
            assetRepository.save(tryAsset);
            log.info("Added 100,000 TRY to customer1");

            // Add some sample stocks
            Asset googleStock = Asset.builder()
                    .customerId(savedCustomer.getId())
                    .assetName("GOOG")
                    .size(new BigDecimal("50"))
                    .usableSize(new BigDecimal("50"))
                    .build();
            assetRepository.save(googleStock);
            log.info("Added 50 GOOG shares to customer1");
        }
    }
}
