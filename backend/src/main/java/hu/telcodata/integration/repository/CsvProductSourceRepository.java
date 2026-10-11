package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.CsvProductSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsvProductSourceRepository extends JpaRepository<CsvProductSource, Long> {

    List<CsvProductSource> findAllByUploadBatchIdOrderByRowNumber(Long uploadBatchId);
}
