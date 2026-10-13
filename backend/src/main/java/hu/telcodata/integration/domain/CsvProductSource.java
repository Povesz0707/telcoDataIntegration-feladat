package hu.telcodata.integration.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
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

@Entity
@Table(name = "csv_product_source")
@Getter
@NoArgsConstructor
public class CsvProductSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_batch_id", nullable = false)
    private UploadBatch uploadBatch;

    private Integer rowNumber;
    private String sku;
    private String productName;
    private BigDecimal grossPriceHuf;
    private Integer stockQty;
    private String brand;
    @Column(columnDefinition = "TEXT")
    private String rawData;

    @Builder
    public CsvProductSource(UploadBatch uploadBatch, Integer rowNumber, String sku,
                            String productName, BigDecimal grossPriceHuf, Integer stockQty,
                            String brand, String rawData) {
        this.uploadBatch = uploadBatch;
        this.rowNumber = rowNumber;
        this.sku = sku;
        this.productName = productName;
        this.grossPriceHuf = grossPriceHuf;
        this.stockQty = stockQty;
        this.brand = brand;
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
