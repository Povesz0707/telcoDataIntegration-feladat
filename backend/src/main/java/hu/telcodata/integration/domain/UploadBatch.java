package hu.telcodata.integration.domain;

import hu.telcodata.integration.domain.enums.SourceType;
import hu.telcodata.integration.domain.enums.UploadStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "upload_batch")
@Getter
@NoArgsConstructor
public class UploadBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String originalFilename;
    private String checksum;
    private Instant uploadedAt;

    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    private Integer rowCount;
    private String errorMessage;

    public UploadBatch(SourceType sourceType, String originalFilename, String checksum,
                       Instant uploadedAt, UploadStatus status, Integer rowCount) {
        this.sourceType = sourceType;
        this.originalFilename = originalFilename;
        this.checksum = checksum;
        this.uploadedAt = uploadedAt;
        this.status = status;
        this.rowCount = rowCount;
    }
}
