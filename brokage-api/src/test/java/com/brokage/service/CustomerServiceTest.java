package com.brokage.service;

import com.brokage.entity.Customer;
import com.brokage.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    @Nested
    @DisplayName("Load User By Username Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should load user successfully")
        void loadUserByUsername_Success() {
            // Given
            Customer customer = Customer.builder()
                    .id(1L)
                    .username("testuser")
                    .password("encodedPassword")
                    .role("CUSTOMER")
                    .build();

            when(customerRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(customer));

            // When
            UserDetails userDetails = customerService.loadUserByUsername("testuser");

            // Then
            assertThat(userDetails.getUsername()).isEqualTo("testuser");
            assertThat(userDetails.getAuthorities()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void loadUserByUsername_NotFound() {
            // Given
            when(customerRepository.findByUsername("unknown"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customerService.loadUserByUsername("unknown"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should load admin user with ADMIN role")
        void loadUserByUsername_Admin() {
            // Given
            Customer admin = Customer.builder()
                    .id(1L)
                    .username("admin")
                    .password("encodedPassword")
                    .role("ADMIN")
                    .build();

            when(customerRepository.findByUsername("admin"))
                    .thenReturn(Optional.of(admin));

            // When
            UserDetails userDetails = customerService.loadUserByUsername("admin");

            // Then
            assertThat(userDetails.getAuthorities().toString()).contains("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Create Customer Tests")
    class CreateCustomerTests {

        @Test
        @DisplayName("Should create customer successfully")
        void createCustomer_Success() {
            // Given
            when(customerRepository.existsByUsername("newuser")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

            Customer savedCustomer = Customer.builder()
                    .id(1L)
                    .username("newuser")
                    .password("encodedPassword")
                    .role("CUSTOMER")
                    .build();

            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

            // When
            Customer result = customerService.createCustomer("newuser", "password", "CUSTOMER");

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("newuser");
            verify(passwordEncoder).encode("password");
        }

        @Test
        @DisplayName("Should throw exception when username exists")
        void createCustomer_UsernameExists() {
            // Given
            when(customerRepository.existsByUsername("existinguser")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer("existinguser", "password", "CUSTOMER"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");
        }
    }

    @Nested
    @DisplayName("Find By Username Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should find customer by username")
        void findByUsername_Found() {
            // Given
            Customer customer = Customer.builder()
                    .id(1L)
                    .username("testuser")
                    .build();

            when(customerRepository.findByUsername("testuser"))
                    .thenReturn(Optional.of(customer));

            // When
            Optional<Customer> result = customerService.findByUsername("testuser");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Should return empty when customer not found")
        void findByUsername_NotFound() {
            // Given
            when(customerRepository.findByUsername("unknown"))
                    .thenReturn(Optional.empty());

            // When
            Optional<Customer> result = customerService.findByUsername("unknown");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find By Id Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should find customer by id")
        void findById_Found() {
            // Given
            Customer customer = Customer.builder()
                    .id(1L)
                    .username("testuser")
                    .build();

            when(customerRepository.findById(1L))
                    .thenReturn(Optional.of(customer));

            // When
            Optional<Customer> result = customerService.findById(1L);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return empty when customer id not found")
        void findById_NotFound() {
            // Given
            when(customerRepository.findById(999L))
                    .thenReturn(Optional.empty());

            // When
            Optional<Customer> result = customerService.findById(999L);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
