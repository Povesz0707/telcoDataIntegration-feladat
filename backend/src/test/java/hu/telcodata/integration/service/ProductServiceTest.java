package hu.telcodata.integration.service;

import hu.telcodata.integration.api.ProductResponseDto;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.ProcessingRun;
import hu.telcodata.integration.domain.enums.ProcessingStatus;
import hu.telcodata.integration.domain.enums.ProductSource;
import hu.telcodata.integration.domain.enums.SourceType;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.exception.ResourceNotFoundException;
import hu.telcodata.integration.mapper.ProductMapper;
import hu.telcodata.integration.repository.NormalizedProductRepository;
import hu.telcodata.integration.repository.ProcessingRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private NormalizedProductRepository productRepository;

    @Mock
    private ProcessingRunRepository processingRunRepository;

    @Mock
    private ProductMapper productMapper;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, processingRunRepository, productMapper);
    }

    @Test
    void findProductsUsesLatestCompletedRunWhenRunIdIsMissing() {
        stubMapper();
        NormalizedProduct product = product("SKU-1", "Cable", "ACME", 100, 2, true);
        ProcessingRun latestRun = mock(ProcessingRun.class);
        when(latestRun.getId()).thenReturn(10L);
        when(processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED))
                .thenReturn(Optional.of(latestRun));
        when(productRepository.findAllByProcessingRunId(10L)).thenReturn(List.of(product));

        List<ProductResponseDto> result = service.findProducts(null, null, null, false);

        assertThat(result).singleElement().extracting(ProductResponseDto::sku).isEqualTo("SKU-1");
        verify(productRepository).findAllByProcessingRunId(10L);
    }

    @Test
    void findProductsFiltersCaseInsensitivelyAndSortsDescending() {
        stubMapper();
        NormalizedProduct cheap = product("SKU-1", "Cable", "ACME", 100, 2, true);
        NormalizedProduct expensive = product("SKU-2", "Router", "ACME", 200, 1, true);
        when(productRepository.findAllByProcessingRunId(10L)).thenReturn(List.of(cheap, expensive));

        List<ProductResponseDto> result = service.findProducts(10L, " acme ", "finalPriceHuf:desc", false);

        assertThat(result).extracting(ProductResponseDto::sku).containsExactly("SKU-2", "SKU-1");
    }

    @Test
    void findProductsCanReturnOnlyValidProductsAndMatchEan() {
        stubMapper();
        NormalizedProduct valid = product("SKU-1", "Cable", "ACME", 100, 2, true);
        NormalizedProduct invalid = product("SKU-2", "Router", "ACME", 200, 1, false);
        when(productRepository.findAllByProcessingRunId(11L)).thenReturn(List.of(invalid, valid));

        List<ProductResponseDto> result = service.findProducts(11L, "EAN-SKU-1", "sku:asc", true);

        assertThat(result).singleElement().extracting(ProductResponseDto::sku).isEqualTo("SKU-1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sku:asc", "name:asc", "manufacturer:asc", "finalPriceHuf:asc",
            "stock:asc", "updatedAt:asc", "source:asc", "ean:asc", "valid:asc"})
    void findProductsAcceptsEverySupportedSortField(String sort) {
        stubMapper();
        NormalizedProduct product = product("SKU-1", "Cable", "ACME", 100, 2, true);
        when(productRepository.findAllByProcessingRunId(12L)).thenReturn(List.of(product));

        assertThat(service.findProducts(12L, "", sort, false)).hasSize(1);
    }

    @Test
    void findProductsRejectsUnsupportedSortField() {
        when(productRepository.findAllByProcessingRunId(13L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findProducts(13L, "", "unknown:asc", false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sort field: unknown");
    }

    @Test
    void findProductsRejectsUnsupportedSortDirection() {
        when(productRepository.findAllByProcessingRunId(14L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findProducts(14L, "", "sku:descending", false))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sort direction: descending");
    }

    @Test
    void findProductsPageUsesTheSameSortValidation() {
        assertThatThrownBy(() -> service.findProductsPage(14L, "", null, null, null, null,
                null, null, null, null, false, 0, 20, "sku:descending"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unsupported sort direction: descending");
    }

    @Test
    void findProductsFailsWhenNoCompletedRunExists() {
        when(processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findProducts(null, "", "", false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No completed processing run exists");
    }

    private NormalizedProduct product(String sku, String name, String manufacturer, int price,
                                      int stock, boolean valid) {
        NormalizedProduct product = new NormalizedProduct(sku, name, manufacturer,
                BigDecimal.valueOf(price), stock, "EAN-" + sku, Instant.parse("2026-01-01T00:00:00Z"),
                ProductSource.CSV, SourceType.CSV);
        product.markValid(valid);
        return product;
    }

    private ProductResponseDto response(NormalizedProduct product) {
        return new ProductResponseDto(product.getSku(), product.getName(), product.getManufacturer(),
                product.getFinalPriceHuf(), product.getStock(), product.getEan(), product.getUpdatedAt(),
                product.getSource(), product.getPrimarySource(), product.isValid(), List.of());
    }

    private void stubMapper() {
        when(productMapper.toResponse(any(NormalizedProduct.class)))
                .thenAnswer(invocation -> response(invocation.getArgument(0)));
    }
}
