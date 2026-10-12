package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.IssueSeverity;
import hu.telcodata.integration.domain.enums.IssueCode;
import hu.telcodata.integration.domain.enums.ProductField;

public record ProductIssueResponseDto(
        IssueCode code,
        ProductField field,
        String message,
        IssueSeverity severity
) {
}
