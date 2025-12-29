package com.brokage.service;

import com.brokage.annotation.Auditable;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String TRY_ASSET = "TRY";

    private final OrderRepository orderRepository;
    private final AssetService assetService;

    @Transactional
    @Auditable(action = "CREATE_ORDER")
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Validate and reserve assets based on order side
        if (request.getOrderSide() == OrderSide.BUY) {
            // For BUY orders, check and reserve TRY balance
            BigDecimal totalCost = request.getSize().multiply(request.getPrice());
            Asset tryAsset = assetService.getAsset(request.getCustomerId(), TRY_ASSET)
                    .orElseThrow(() -> new InsufficientBalanceException("No TRY balance found"));

            if (tryAsset.getUsableSize().compareTo(totalCost) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient TRY balance. Required: " + totalCost +
                                ", Available: " + tryAsset.getUsableSize());
            }

            // Reserve TRY
            assetService.reserveAsset(request.getCustomerId(), TRY_ASSET, totalCost);

        } else {
            // For SELL orders, check and reserve the asset
            Asset asset = assetService.getAsset(request.getCustomerId(), request.getAssetName())
                    .orElseThrow(() -> new InsufficientBalanceException(
                            "No " + request.getAssetName() + " balance found"));

            if (asset.getUsableSize().compareTo(request.getSize()) < 0) {
                throw new InsufficientBalanceException(
                        "Insufficient " + request.getAssetName() + " balance. Required: " +
                                request.getSize() + ", Available: " + asset.getUsableSize());
            }

            // Reserve asset
            assetService.reserveAsset(request.getCustomerId(), request.getAssetName(), request.getSize());
        }

        // Create order
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .assetName(request.getAssetName())
                .orderSide(request.getOrderSide())
                .size(request.getSize())
                .price(request.getPrice())
                .status(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    public List<OrderResponse> listOrders(Long customerId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Order> orders;

        if (startDate != null && endDate != null) {
            orders = orderRepository.findByCustomerIdAndCreateDateBetween(customerId, startDate, endDate);
        } else {
            orders = orderRepository.findByCustomerId(customerId);
        }

        return orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> listAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    @Auditable(action = "CANCEL_ORDER")
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be canceled. Current status: " + order.getStatus());
        }

        // Release reserved assets
        if (order.getOrderSide() == OrderSide.BUY) {
            // Release TRY
            BigDecimal totalCost = order.getSize().multiply(order.getPrice());
            assetService.releaseAsset(order.getCustomerId(), TRY_ASSET, totalCost);
        } else {
            // Release the asset
            assetService.releaseAsset(order.getCustomerId(), order.getAssetName(), order.getSize());
        }

        order.setStatus(OrderStatus.CANCELED);
        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional
    @Auditable(action = "MATCH_ORDER")
    public OrderResponse matchOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be matched. Current status: " + order.getStatus());
        }

        BigDecimal totalValue = order.getSize().multiply(order.getPrice());

        if (order.getOrderSide() == OrderSide.BUY) {
            // BUY order:
            // - Decrease TRY size (already reserved usableSize during order creation)
            // - Increase target asset size and usableSize
            assetService.updateAssetOnMatch(
                    order.getCustomerId(),
                    TRY_ASSET,
                    totalValue.negate(), // size decrease
                    BigDecimal.ZERO // usableSize already decreased during reservation
            );

            assetService.updateAssetOnMatch(
                    order.getCustomerId(),
                    order.getAssetName(),
                    order.getSize(), // size increase
                    order.getSize() // usableSize increase
            );

        } else {
            // SELL order:
            // - Decrease asset size (already reserved usableSize during order creation)
            // - Increase TRY size and usableSize
            assetService.updateAssetOnMatch(
                    order.getCustomerId(),
                    order.getAssetName(),
                    order.getSize().negate(), // size decrease
                    BigDecimal.ZERO // usableSize already decreased during reservation
            );

            assetService.updateAssetOnMatch(
                    order.getCustomerId(),
                    TRY_ASSET,
                    totalValue, // size increase
                    totalValue // usableSize increase
            );
        }

        order.setStatus(OrderStatus.MATCHED);
        Order savedOrder = orderRepository.save(order);
        return OrderResponse.fromEntity(savedOrder);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.fromEntity(order);
    }
}
