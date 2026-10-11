package hu.telcodata.integration.normalization;

import hu.telcodata.integration.domain.IssueCode;
import hu.telcodata.integration.domain.SourceType;

import java.math.BigDecimal;
import java.time.Instant;

public record SourceCandidate(
        SourceType sourceType,
        String rawSku,
        String normalizedSku,
        String name,
        String manufacturer,
        BigDecimal finalPriceHuf,
        Integer stock,
        String ean,
        Instant updatedAt,
        IssueCode sourceDataIssue,
        int rowNumber
) {
}
