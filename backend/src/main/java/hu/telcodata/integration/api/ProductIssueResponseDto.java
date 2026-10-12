package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.enums.IssueSeverity;
import hu.telcodata.integration.domain.enums.IssueCode;

public record ProductIssueResponseDto(
        IssueCode code,
        String field,
        String message,
        IssueSeverity severity
) {
}
