package hu.telcodata.integration.domain;

import hu.telcodata.integration.domain.enums.IssueCode;
import hu.telcodata.integration.domain.enums.IssueSeverity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "normalized_product_issue")
@Getter
@NoArgsConstructor
public class NormalizedProductIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "normalized_product_id", nullable = false)
    private NormalizedProduct product;

    @Enumerated(EnumType.STRING)
    private IssueCode code;
    private String field;
    private String message;

    @Enumerated(EnumType.STRING)
    private IssueSeverity severity;

    public NormalizedProductIssue(NormalizedProduct product, IssueCode code, String field,
                                  String message, IssueSeverity severity) {
        this.product = product;
        this.code = code;
        this.field = field;
        this.message = message;
        this.severity = severity;
    }
}
