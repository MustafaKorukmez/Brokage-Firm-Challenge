package com.brokage.service;

import com.brokage.dto.request.CreateOrderRequest;
import com.brokage.dto.response.OrderResponse;
import com.brokage.entity.Asset;
import com.brokage.entity.Order;
import com.brokage.enums.OrderSide;
import com.brokage.enums.OrderStatus;
import com.brokage.exception.InsufficientBalanceException;
import com.brokage.exception.InvalidOrderStateException;
import com.brokage.exception.OrderNotFoundException;
import com.brokage.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AssetService assetService;

    @InjectMocks
    private OrderService orderService;

    private static final Long CUSTOMER_ID = 1L;
    private static final String TRY_ASSET = "TRY";
    private static final String GOOGLE_ASSET = "GOOG";

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create BUY order successfully when sufficient TRY balance")
        void createBuyOrder_Success() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .build();

            Asset tryAsset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName(TRY_ASSET)
                    .size(new BigDecimal("5000"))
                    .usableSize(new BigDecimal("5000"))
                    .build();

            Order savedOrder = Order.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .status(OrderStatus.PENDING)
                    .createDate(LocalDateTime.now())
                    .build();

            when(assetService.getAsset(CUSTOMER_ID, TRY_ASSET)).thenReturn(Optional.of(tryAsset));
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            // When
            OrderResponse response = orderService.createOrder(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getOrderSide()).isEqualTo(OrderSide.BUY);
            verify(assetService).reserveAsset(CUSTOMER_ID, TRY_ASSET, new BigDecimal("1000"));
        }

        @Test
        @DisplayName("Should throw InsufficientBalanceException when TRY balance is low")
        void createBuyOrder_InsufficientBalance() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .build();

            Asset tryAsset = Asset.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName(TRY_ASSET)
                    .size(new BigDecimal("500"))
                    .usableSize(new BigDecimal("500"))
                    .build();

            when(assetService.getAsset(CUSTOMER_ID, TRY_ASSET)).thenReturn(Optional.of(tryAsset));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient TRY balance");
        }

        @Test
        @DisplayName("Should create SELL order successfully when sufficient asset balance")
        void createSellOrder_Success() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.SELL)
                    .size(new BigDecimal("5"))
                    .price(new BigDecimal("150"))
                    .build();

            Asset googleAsset = Asset.builder()
                    .id(2L)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .size(new BigDecimal("50"))
                    .usableSize(new BigDecimal("50"))
                    .build();

            Order savedOrder = Order.builder()
                    .id(2L)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.SELL)
                    .size(new BigDecimal("5"))
                    .price(new BigDecimal("150"))
                    .status(OrderStatus.PENDING)
                    .createDate(LocalDateTime.now())
                    .build();

            when(assetService.getAsset(CUSTOMER_ID, GOOGLE_ASSET)).thenReturn(Optional.of(googleAsset));
            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

            // When
            OrderResponse response = orderService.createOrder(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.getOrderSide()).isEqualTo(OrderSide.SELL);
            verify(assetService).reserveAsset(CUSTOMER_ID, GOOGLE_ASSET, new BigDecimal("5"));
        }

        @Test
        @DisplayName("Should throw InsufficientBalanceException when asset balance is low for SELL")
        void createSellOrder_InsufficientBalance() {
            // Given
            CreateOrderRequest request = CreateOrderRequest.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.SELL)
                    .size(new BigDecimal("100"))
                    .price(new BigDecimal("150"))
                    .build();

            Asset googleAsset = Asset.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .size(new BigDecimal("50"))
                    .usableSize(new BigDecimal("50"))
                    .build();

            when(assetService.getAsset(CUSTOMER_ID, GOOGLE_ASSET)).thenReturn(Optional.of(googleAsset));

            // When & Then
            assertThatThrownBy(() -> orderService.createOrder(request))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .hasMessageContaining("Insufficient GOOG balance");
        }
    }

    @Nested
    @DisplayName("Cancel Order Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should cancel PENDING BUY order and release TRY")
        void cancelBuyOrder_Success() {
            // Given
            Long orderId = 1L;
            Order order = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .status(OrderStatus.PENDING)
                    .createDate(LocalDateTime.now())
                    .build();

            Order canceledOrder = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .status(OrderStatus.CANCELED)
                    .createDate(LocalDateTime.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(canceledOrder);

            // When
            OrderResponse response = orderService.cancelOrder(orderId);

            // Then
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELED);
            verify(assetService).releaseAsset(CUSTOMER_ID, TRY_ASSET, new BigDecimal("1000"));
        }

        @Test
        @DisplayName("Should throw InvalidOrderStateException when canceling non-PENDING order")
        void cancelOrder_InvalidState() {
            // Given
            Long orderId = 1L;
            Order order = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .status(OrderStatus.MATCHED)
                    .createDate(LocalDateTime.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                    .isInstanceOf(InvalidOrderStateException.class)
                    .hasMessageContaining("Only PENDING orders can be canceled");
        }

        @Test
        @DisplayName("Should throw OrderNotFoundException when order does not exist")
        void cancelOrder_NotFound() {
            // Given
            Long orderId = 999L;
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> orderService.cancelOrder(orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Match Order Tests")
    class MatchOrderTests {

        @Test
        @DisplayName("Should match BUY order and update assets correctly")
        void matchBuyOrder_Success() {
            // Given
            Long orderId = 1L;
            Order order = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .status(OrderStatus.PENDING)
                    .createDate(LocalDateTime.now())
                    .build();

            Order matchedOrder = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.BUY)
                    .size(new BigDecimal("10"))
                    .price(new BigDecimal("100"))
                    .status(OrderStatus.MATCHED)
                    .createDate(LocalDateTime.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(matchedOrder);

            // When
            OrderResponse response = orderService.matchOrder(orderId);

            // Then
            assertThat(response.getStatus()).isEqualTo(OrderStatus.MATCHED);
            verify(assetService).updateAssetOnMatch(CUSTOMER_ID, TRY_ASSET,
                    new BigDecimal("-1000"), BigDecimal.ZERO);
            verify(assetService).updateAssetOnMatch(CUSTOMER_ID, GOOGLE_ASSET,
                    new BigDecimal("10"), new BigDecimal("10"));
        }

        @Test
        @DisplayName("Should match SELL order and update assets correctly")
        void matchSellOrder_Success() {
            // Given
            Long orderId = 2L;
            Order order = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.SELL)
                    .size(new BigDecimal("5"))
                    .price(new BigDecimal("150"))
                    .status(OrderStatus.PENDING)
                    .createDate(LocalDateTime.now())
                    .build();

            Order matchedOrder = Order.builder()
                    .id(orderId)
                    .customerId(CUSTOMER_ID)
                    .assetName(GOOGLE_ASSET)
                    .orderSide(OrderSide.SELL)
                    .size(new BigDecimal("5"))
                    .price(new BigDecimal("150"))
                    .status(OrderStatus.MATCHED)
                    .createDate(LocalDateTime.now())
                    .build();

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(matchedOrder);

            // When
            OrderResponse response = orderService.matchOrder(orderId);

            // Then
            assertThat(response.getStatus()).isEqualTo(OrderStatus.MATCHED);
            verify(assetService).updateAssetOnMatch(CUSTOMER_ID, GOOGLE_ASSET,
                    new BigDecimal("-5"), BigDecimal.ZERO);
            verify(assetService).updateAssetOnMatch(CUSTOMER_ID, TRY_ASSET,
                    new BigDecimal("750"), new BigDecimal("750"));
        }
    }
}
