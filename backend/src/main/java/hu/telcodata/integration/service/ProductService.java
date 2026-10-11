package hu.telcodata.integration.service;

import hu.telcodata.integration.api.ProductResponseDto;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.ProcessingStatus;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.exception.ResourceNotFoundException;
import hu.telcodata.integration.mapper.ProductMapper;
import hu.telcodata.integration.normalization.ProductFieldNames;
import hu.telcodata.integration.repository.NormalizedProductRepository;
import hu.telcodata.integration.repository.ProcessingRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final NormalizedProductRepository productRepository;
    private final ProcessingRunRepository processingRunRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductResponseDto> findProducts(Long runId, String filter, String sort, boolean onlyValid) {
        Long effectiveRunId = runId == null ? latestRunId() : runId;
        List<NormalizedProduct> products = productRepository.findAllByProcessingRunId(effectiveRunId);
        String normalizedFilter = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);

        Comparator<NormalizedProduct> comparator = comparator(sort);
        return products.stream()
                .filter(product -> !onlyValid || product.isValid())
                .filter(product -> matches(product, normalizedFilter))
                .sorted(comparator)
                .map(productMapper::toResponse)
                .toList();
    }

    private Long latestRunId() {
        return processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED)
                .map(run -> run.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No completed processing run exists"));
    }

    private boolean matches(NormalizedProduct product, String filter) {
        if (filter.isBlank()) {
            return true;
        }
        return contains(product.getSku(), filter)
                || contains(product.getName(), filter)
                || contains(product.getManufacturer(), filter)
                || contains(product.getEan(), filter);
    }

    private boolean contains(String value, String filter) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(filter);
    }

    private Comparator<NormalizedProduct> comparator(String sort) {
        if (sort == null || sort.isBlank()) {
            return Comparator.comparing(product -> product.getSku() == null ? "" : product.getSku());
        }
        String[] parts = sort.split(":", 2);
        Function<NormalizedProduct, Comparable<Object>> key = switch (parts[0]) {
            case ProductFieldNames.SKU -> product -> comparable(product.getSku());
            case ProductFieldNames.NAME -> product -> comparable(product.getName());
            case ProductFieldNames.MANUFACTURER -> product -> comparable(product.getManufacturer());
            case ProductFieldNames.FINAL_PRICE_HUF -> product -> comparable(product.getFinalPriceHuf());
            case ProductFieldNames.STOCK -> product -> comparable(product.getStock());
            case ProductFieldNames.UPDATED_AT -> product -> comparable(product.getUpdatedAt());
            case ProductFieldNames.SOURCE -> product -> comparable(product.getSource());
            default -> throw new BadRequestException("Unsupported sort field: " + parts[0]);
        };
        Comparator<NormalizedProduct> comparator = Comparator.comparing(key,
                Comparator.nullsLast(Comparator.naturalOrder()));
        return parts.length == 2 && "desc".equalsIgnoreCase(parts[1]) ? comparator.reversed() : comparator;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<?>> Comparable<Object> comparable(T value) {
        return value == null ? null : (Comparable<Object>) value;
    }
}
