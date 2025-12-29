package com.brokage.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "assets", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "customer_id", "asset_name" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal size;

    @Column(name = "usable_size", nullable = false, precision = 18, scale = 8)
    private BigDecimal usableSize;

    @Version
    private Long version;
}
