package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.UploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadBatchRepository extends JpaRepository<UploadBatch, Long> {
}
