package com.brokage.service;

import com.brokage.dto.response.AssetResponse;
import com.brokage.entity.Asset;
import com.brokage.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    public List<AssetResponse> getAssetsByCustomerId(Long customerId) {
        return assetRepository.findByCustomerId(customerId)
                .stream()
                .map(AssetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<Asset> getAsset(Long customerId, String assetName) {
        return assetRepository.findByCustomerIdAndAssetName(customerId, assetName);
    }

    @Transactional
    public Asset getOrCreateAsset(Long customerId, String assetName) {
        return assetRepository.findByCustomerIdAndAssetName(customerId, assetName)
                .orElseGet(() -> assetRepository.save(Asset.builder()
                        .customerId(customerId)
                        .assetName(assetName)
                        .size(BigDecimal.ZERO)
                        .usableSize(BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public void reserveAsset(Long customerId, String assetName, BigDecimal amount) {
        Asset asset = getAsset(customerId, assetName)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetName));
        asset.setUsableSize(asset.getUsableSize().subtract(amount));
        assetRepository.save(asset);
    }

    @Transactional
    public void releaseAsset(Long customerId, String assetName, BigDecimal amount) {
        Asset asset = getAsset(customerId, assetName)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetName));
        asset.setUsableSize(asset.getUsableSize().add(amount));
        assetRepository.save(asset);
    }

    @Transactional
    public void updateAssetOnMatch(Long customerId, String assetName,
            BigDecimal sizeChange, BigDecimal usableSizeChange) {
        Asset asset = getOrCreateAsset(customerId, assetName);
        asset.setSize(asset.getSize().add(sizeChange));
        asset.setUsableSize(asset.getUsableSize().add(usableSizeChange));
        assetRepository.save(asset);
    }

    @Transactional
    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }
}
