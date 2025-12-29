package com.brokage.repository;

import com.brokage.entity.Order;
import com.brokage.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByCustomerIdAndCreateDateBetween(
            Long customerId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    List<Order> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    List<Order> findByStatus(OrderStatus status);
}
