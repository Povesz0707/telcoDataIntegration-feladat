package hu.telcodata.integration.domain;

import hu.telcodata.integration.domain.enums.ProcessingStatus;
import hu.telcodata.integration.domain.enums.SourceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "processing_run")
@Getter
@NoArgsConstructor
public class ProcessingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "csv_batch_id", nullable = false)
    private UploadBatch csvBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "json_batch_id", nullable = false)
    private UploadBatch jsonBatch;

    @Enumerated(EnumType.STRING)
    private SourceType primarySource;

    private boolean fallbackMissingValues;
    private Instant startedAt;
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    private Integer validCount;
    private Integer invalidCount;

    @OneToMany(mappedBy = "processingRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NormalizedProduct> products = new ArrayList<>();

    public ProcessingRun(UploadBatch csvBatch, UploadBatch jsonBatch, SourceType primarySource,
                         boolean fallbackMissingValues) {
        this.csvBatch = csvBatch;
        this.jsonBatch = jsonBatch;
        this.primarySource = primarySource;
        this.fallbackMissingValues = fallbackMissingValues;
        this.startedAt = Instant.now();
        this.status = ProcessingStatus.RUNNING;
    }

    public void complete(int validCount, int invalidCount) {
        this.finishedAt = Instant.now();
        this.status = ProcessingStatus.COMPLETED;
        this.validCount = validCount;
        this.invalidCount = invalidCount;
    }

    public void fail() {
        this.finishedAt = Instant.now();
        this.status = ProcessingStatus.FAILED;
    }

    public void addProduct(NormalizedProduct product) {
        products.add(product);
        product.attachTo(this);
    }
}
