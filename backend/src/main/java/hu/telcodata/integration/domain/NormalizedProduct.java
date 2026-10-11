package hu.telcodata.integration.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "normalized_product")
@Getter
@NoArgsConstructor
public class NormalizedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processing_run_id", nullable = false)
    private ProcessingRun processingRun;

    private String sku;
    private String name;
    private String manufacturer;
    private BigDecimal finalPriceHuf;
    private Integer stock;
    private String ean;
    private Instant updatedAt;
    private String source;
    private String primarySource;
    private boolean valid;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NormalizedProductIssue> issues = new ArrayList<>();

    public NormalizedProduct(String sku, String name, String manufacturer, BigDecimal finalPriceHuf,
                             Integer stock, String ean, Instant updatedAt, String source,
                             String primarySource) {
        this.sku = sku;
        this.name = name;
        this.manufacturer = manufacturer;
        this.finalPriceHuf = finalPriceHuf;
        this.stock = stock;
        this.ean = ean;
        this.updatedAt = updatedAt;
        this.source = source;
        this.primarySource = primarySource;
    }

    void attachTo(ProcessingRun processingRun) {
        this.processingRun = processingRun;
    }

    public void addIssue(IssueCode code, String field, String message, IssueSeverity severity) {
        issues.add(new NormalizedProductIssue(this, code, field, message, severity));
    }

    public void markValid(boolean valid) {
        this.valid = valid;
    }
}
