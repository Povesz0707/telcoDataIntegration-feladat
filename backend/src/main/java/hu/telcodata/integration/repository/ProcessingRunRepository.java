package hu.telcodata.integration.repository;

import hu.telcodata.integration.domain.ProcessingRun;
import hu.telcodata.integration.domain.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface ProcessingRunRepository extends JpaRepository<ProcessingRun, Long> {

    Optional<ProcessingRun> findTopByStatusOrderByStartedAtDesc(ProcessingStatus status);

    List<ProcessingRun> findAllByStatusOrderByStartedAtDesc(ProcessingStatus status);
}
