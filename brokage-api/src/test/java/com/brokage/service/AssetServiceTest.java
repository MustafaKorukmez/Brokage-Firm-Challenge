package com.brokage.service;

import com.brokage.dto.response.AssetResponse;
import com.brokage.entity.Asset;
import com.brokage.repository.AssetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    private static final Long CUSTOMER_ID = 1L;

    @Nested
    @DisplayName("Get Assets By Customer Tests")
    class GetAssetsByCustomerTests {

        @Test
        @DisplayName("Should return list of assets for customer")
        void getAssetsByCustomerId_Success() {
            // Given
            Asset tryAsset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName("TRY")
                    .size(new BigDecimal("10000"))
                    .usableSize(new BigDecimal("10000"))
                    .build();

            Asset googAsset = Asset.builder()
                    .id(2L)
                    .customerId(CUSTOMER_ID)
                    .assetName("GOOG")
                    .size(new BigDecimal("50"))
                    .usableSize(new BigDecimal("50"))
                    .build();

            when(assetRepository.findByCustomerId(CUSTOMER_ID))
                    .thenReturn(Arrays.asList(tryAsset, googAsset));

            // When
            List<AssetResponse> result = assetService.getAssetsByCustomerId(CUSTOMER_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getAssetName()).isEqualTo("TRY");
            assertThat(result.get(1).getAssetName()).isEqualTo("GOOG");
        }

        @Test
        @DisplayName("Should return empty list when customer has no assets")
        void getAssetsByCustomerId_Empty() {
            // Given
            when(assetRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

            // When
            List<AssetResponse> result = assetService.getAssetsByCustomerId(CUSTOMER_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Asset Tests")
    class GetAssetTests {

        @Test
        @DisplayName("Should return asset when exists")
        void getAsset_Found() {
            // Given
            Asset asset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName("TRY")
                    .size(new BigDecimal("10000"))
                    .usableSize(new BigDecimal("10000"))
                    .build();

            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "TRY"))
                    .thenReturn(Optional.of(asset));

            // When
            Optional<Asset> result = assetService.getAsset(CUSTOMER_ID, "TRY");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAssetName()).isEqualTo("TRY");
        }

        @Test
        @DisplayName("Should return empty when asset not found")
        void getAsset_NotFound() {
            // Given
            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "AAPL"))
                    .thenReturn(Optional.empty());

            // When
            Optional<Asset> result = assetService.getAsset(CUSTOMER_ID, "AAPL");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Or Create Asset Tests")
    class GetOrCreateAssetTests {

        @Test
        @DisplayName("Should return existing asset")
        void getOrCreateAsset_Existing() {
            // Given
            Asset existingAsset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName("GOOG")
                    .size(new BigDecimal("50"))
                    .usableSize(new BigDecimal("50"))
                    .build();

            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "GOOG"))
                    .thenReturn(Optional.of(existingAsset));

            // When
            Asset result = assetService.getOrCreateAsset(CUSTOMER_ID, "GOOG");

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create new asset when not exists")
        void getOrCreateAsset_Create() {
            // Given
            Asset newAsset = Asset.builder()
                    .id(10L)
                    .customerId(CUSTOMER_ID)
                    .assetName("AAPL")
                    .size(BigDecimal.ZERO)
                    .usableSize(BigDecimal.ZERO)
                    .build();

            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "AAPL"))
                    .thenReturn(Optional.empty());
            when(assetRepository.save(any(Asset.class))).thenReturn(newAsset);

            // When
            Asset result = assetService.getOrCreateAsset(CUSTOMER_ID, "AAPL");

            // Then
            assertThat(result.getAssetName()).isEqualTo("AAPL");
            verify(assetRepository).save(any(Asset.class));
        }
    }

    @Nested
    @DisplayName("Reserve Asset Tests")
    class ReserveAssetTests {

        @Test
        @DisplayName("Should reserve asset successfully")
        void reserveAsset_Success() {
            // Given
            Asset asset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName("TRY")
                    .size(new BigDecimal("10000"))
                    .usableSize(new BigDecimal("10000"))
                    .build();

            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "TRY"))
                    .thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenReturn(asset);

            // When
            assetService.reserveAsset(CUSTOMER_ID, "TRY", new BigDecimal("1000"));

            // Then
            verify(assetRepository)
                    .save(argThat(savedAsset -> savedAsset.getUsableSize().compareTo(new BigDecimal("9000")) == 0));
        }

        @Test
        @DisplayName("Should throw exception when asset not found for reservation")
        void reserveAsset_NotFound() {
            // Given
            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "AAPL"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> assetService.reserveAsset(CUSTOMER_ID, "AAPL", new BigDecimal("10")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found");
        }
    }

    @Nested
    @DisplayName("Release Asset Tests")
    class ReleaseAssetTests {

        @Test
        @DisplayName("Should release asset successfully")
        void releaseAsset_Success() {
            // Given
            Asset asset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName("TRY")
                    .size(new BigDecimal("10000"))
                    .usableSize(new BigDecimal("9000"))
                    .build();

            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "TRY"))
                    .thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenReturn(asset);

            // When
            assetService.releaseAsset(CUSTOMER_ID, "TRY", new BigDecimal("1000"));

            // Then
            verify(assetRepository)
                    .save(argThat(savedAsset -> savedAsset.getUsableSize().compareTo(new BigDecimal("10000")) == 0));
        }

        @Test
        @DisplayName("Should throw exception when asset not found for release")
        void releaseAsset_NotFound() {
            // Given
            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "AAPL"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> assetService.releaseAsset(CUSTOMER_ID, "AAPL", new BigDecimal("10")))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found");
        }
    }

    @Nested
    @DisplayName("Update Asset On Match Tests")
    class UpdateAssetOnMatchTests {

        @Test
        @DisplayName("Should update asset size and usable size on match")
        void updateAssetOnMatch_Success() {
            // Given
            Asset asset = Asset.builder()
                    .id(1L)
                    .customerId(CUSTOMER_ID)
                    .assetName("GOOG")
                    .size(new BigDecimal("50"))
                    .usableSize(new BigDecimal("50"))
                    .build();

            when(assetRepository.findByCustomerIdAndAssetName(CUSTOMER_ID, "GOOG"))
                    .thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenReturn(asset);

            // When
            assetService.updateAssetOnMatch(CUSTOMER_ID, "GOOG",
                    new BigDecimal("10"), new BigDecimal("10"));

            // Then
            verify(assetRepository)
                    .save(argThat(savedAsset -> savedAsset.getSize().compareTo(new BigDecimal("60")) == 0 &&
                            savedAsset.getUsableSize().compareTo(new BigDecimal("60")) == 0));
        }
    }

    @Nested
    @DisplayName("Save Asset Tests")
    class SaveAssetTests {

        @Test
        @DisplayName("Should save asset")
        void save_Success() {
            // Given
            Asset asset = Asset.builder()
                    .customerId(CUSTOMER_ID)
                    .assetName("MSFT")
                    .size(new BigDecimal("100"))
                    .usableSize(new BigDecimal("100"))
                    .build();

            Asset savedAsset = Asset.builder()
                    .id(5L)
                    .customerId(CUSTOMER_ID)
                    .assetName("MSFT")
                    .size(new BigDecimal("100"))
                    .usableSize(new BigDecimal("100"))
                    .build();

            when(assetRepository.save(asset)).thenReturn(savedAsset);

            // When
            Asset result = assetService.save(asset);

            // Then
            assertThat(result.getId()).isEqualTo(5L);
        }
    }
}
