package hu.telcodata.integration.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "json_product_source")
@Getter
@NoArgsConstructor
public class JsonProductSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_batch_id", nullable = false)
    private UploadBatch uploadBatch;

    private Integer rowNumber;
    private String sourceId;
    private String name;
    private BigDecimal netPrice;
    private String currency;
    private BigDecimal vatRate;
    private Integer quantityAvailable;
    private String manufacturer;
    private String ean;
    private OffsetDateTime updatedAt;
    private String rawData;

    @Builder
    public JsonProductSource(UploadBatch uploadBatch, Integer rowNumber, String sourceId,
                             String name, BigDecimal netPrice, String currency,
                             BigDecimal vatRate, Integer quantityAvailable, String manufacturer,
                             String ean, OffsetDateTime updatedAt, String rawData) {
        this.uploadBatch = uploadBatch;
        this.rowNumber = rowNumber;
        this.sourceId = sourceId;
        this.name = name;
        this.netPrice = netPrice;
        this.currency = currency;
        this.vatRate = vatRate;
        this.quantityAvailable = quantityAvailable;
        this.manufacturer = manufacturer;
        this.ean = ean;
        this.updatedAt = updatedAt;
        this.rawData = rawData;
    }

    public void attachTo(UploadBatch uploadBatch, int rowNumber) {
        this.uploadBatch = uploadBatch;
        this.rowNumber = rowNumber;
    }

    public void attachTo(UploadBatch uploadBatch, int rowNumber, String rawData) {
        attachTo(uploadBatch, rowNumber);
        this.rawData = rawData;
    }
}
