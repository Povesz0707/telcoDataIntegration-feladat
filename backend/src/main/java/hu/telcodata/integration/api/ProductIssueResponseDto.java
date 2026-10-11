package hu.telcodata.integration.api;

import hu.telcodata.integration.domain.IssueSeverity;
import hu.telcodata.integration.domain.IssueCode;

public record ProductIssueResponseDto(
        IssueCode code,
        String field,
        String message,
        IssueSeverity severity
) {
}
