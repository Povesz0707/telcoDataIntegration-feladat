package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.NormalizedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface NormalizedProductRepository extends JpaRepository<NormalizedProduct, Long>,
        JpaSpecificationExecutor<NormalizedProduct> {

    List<NormalizedProduct> findAllByProcessingRunId(Long processingRunId);
}
