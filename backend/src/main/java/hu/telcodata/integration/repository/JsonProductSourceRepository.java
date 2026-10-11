package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.JsonProductSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JsonProductSourceRepository extends JpaRepository<JsonProductSource, Long> {

    List<JsonProductSource> findAllByUploadBatchIdOrderByRowNumber(Long uploadBatchId);
}
