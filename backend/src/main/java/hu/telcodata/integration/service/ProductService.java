package hu.telcodata.integration.service;

import hu.telcodata.integration.api.ProductResponseDto;
import hu.telcodata.integration.api.ProductPageResponseDto;
import hu.telcodata.integration.constant.MessageConstants;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.enums.ProcessingStatus;
import hu.telcodata.integration.domain.enums.ProductField;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.exception.ResourceNotFoundException;
import hu.telcodata.integration.mapper.ProductMapper;
import hu.telcodata.integration.repository.NormalizedProductRepository;
import hu.telcodata.integration.repository.NormalizedProductSpecifications;
import hu.telcodata.integration.repository.ProcessingRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final NormalizedProductRepository productRepository;
    private final ProcessingRunRepository processingRunRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public List<ProductResponseDto> findProducts(Long runId, String filter, String sort, boolean onlyValid) {
        Long effectiveRunId = runId == null ? latestRunId() : runId;
        log.info("Product list requested: runId={}, effectiveRunId={}, filterPresent={}, sort={}, onlyValid={}",
                runId, effectiveRunId, filter != null && !filter.isBlank(), sort, onlyValid);
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

    @Transactional(readOnly = true)
    public ProductPageResponseDto findProductsPage(Long runId, String filter, String sku,
                                                    String name, String manufacturer,
                                                    String finalPriceHuf, String stock, String ean,
                                                    String source, Boolean valid, boolean onlyValid,
                                                    int page, int size, String sort) {
        validatePageRequest(page, size);
        Long effectiveRunId = runId == null ? latestRunId() : runId;
        log.info("Product page requested: runId={}, effectiveRunId={}, page={}, size={}, filterPresent={}, sort={}, onlyValid={}",
                runId, effectiveRunId, page, size, filter != null && !filter.isBlank(), sort, onlyValid);
        Page<NormalizedProduct> products = productRepository.findAll(
                NormalizedProductSpecifications.byQuery(effectiveRunId, filter, sku, name, manufacturer,
                        finalPriceHuf, stock, ean, source, valid, onlyValid),
                PageRequest.of(page, size, toDatabaseSort(sort)));
        Page<ProductResponseDto> responsePage = products.map(productMapper::toResponse);
        return new ProductPageResponseDto(responsePage.getContent(), responsePage.getNumber(),
                responsePage.getSize(), responsePage.getTotalElements(), responsePage.getTotalPages());
    }

    private Long latestRunId() {
        return processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED)
                .map(run -> run.getId())
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.NO_COMPLETED_PROCESSING_RUN));
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
        ProductField field = ProductField.findByPropertyName(parts[0])
                .orElseThrow(() -> new BadRequestException(MessageConstants.unsupportedSortField(parts[0])));
        Function<NormalizedProduct, Comparable<Object>> key = switch (field) {
            case SKU -> product -> comparable(product.getSku());
            case NAME -> product -> comparable(product.getName());
            case MANUFACTURER -> product -> comparable(product.getManufacturer());
            case FINAL_PRICE_HUF -> product -> comparable(product.getFinalPriceHuf());
            case STOCK -> product -> comparable(product.getStock());
            case EAN -> product -> comparable(product.getEan());
            case UPDATED_AT -> product -> comparable(product.getUpdatedAt());
            case SOURCE -> product -> comparable(product.getSource());
            case VALID -> product -> comparable(product.isValid());
        };
        Comparator<NormalizedProduct> comparator = Comparator.comparing(key,
                Comparator.nullsLast(Comparator.naturalOrder()));
        return parts.length == 2 && "desc".equalsIgnoreCase(parts[1]) ? comparator.reversed() : comparator;
    }

    private Sort toDatabaseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, ProductField.SKU.propertyName());
        }
        String[] parts = sort.split(":", 2);
        ProductField field = ProductField.findByPropertyName(parts[0])
                .orElseThrow(() -> new BadRequestException(MessageConstants.unsupportedSortField(parts[0])));
        Sort.Direction direction;
        if (parts.length == 1 || "asc".equalsIgnoreCase(parts[1])) {
            direction = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(parts[1])) {
            direction = Sort.Direction.DESC;
        } else {
            throw new BadRequestException(MessageConstants.unsupportedSortDirection(parts[1]));
        }
        return Sort.by(direction, field.propertyName());
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BadRequestException(MessageConstants.NEGATIVE_PAGE);
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException(MessageConstants.INVALID_PAGE_SIZE);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<?>> Comparable<Object> comparable(T value) {
        return value == null ? null : (Comparable<Object>) value;
    }
}
