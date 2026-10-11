package hu.telcodata.integration.service;

import hu.telcodata.integration.api.ProcessingRunRequestDto;
import hu.telcodata.integration.api.ProcessingRunResponseDto;
import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.ProcessingRun;
import hu.telcodata.integration.domain.ProcessingStatus;
import hu.telcodata.integration.domain.SourceType;
import hu.telcodata.integration.domain.UploadBatch;
import hu.telcodata.integration.domain.UploadStatus;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.exception.ResourceNotFoundException;
import hu.telcodata.integration.normalization.ProductMergeService;
import hu.telcodata.integration.repository.CsvProductSourceRepository;
import hu.telcodata.integration.repository.JsonProductSourceRepository;
import hu.telcodata.integration.repository.ProcessingRunRepository;
import hu.telcodata.integration.repository.UploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final UploadBatchRepository uploadBatchRepository;
    private final CsvProductSourceRepository csvProductSourceRepository;
    private final JsonProductSourceRepository jsonProductSourceRepository;
    private final ProcessingRunRepository processingRunRepository;
    private final ProductMergeService productMergeService;

    @Transactional
    public ProcessingRunResponseDto process(ProcessingRunRequestDto request) {
        UploadBatch csvBatch = getBatch(request.csvBatchId(), SourceType.CSV);
        UploadBatch jsonBatch = getBatch(request.jsonBatchId(), SourceType.JSON);

        List<CsvProductSource> csvRecords = csvProductSourceRepository
                .findAllByUploadBatchIdOrderByRowNumber(csvBatch.getId());
        List<JsonProductSource> jsonRecords = jsonProductSourceRepository
                .findAllByUploadBatchIdOrderByRowNumber(jsonBatch.getId());

        ProcessingRun run = processingRunRepository.save(new ProcessingRun(
                csvBatch, jsonBatch, request.primarySource(), request.fallbackMissingValues()));

        List<NormalizedProduct> products = productMergeService.merge(
                csvRecords, jsonRecords, request.primarySource(), request.fallbackMissingValues());
        products.forEach(run::addProduct);

        int validCount = (int) products.stream().filter(NormalizedProduct::isValid).count();
        run.complete(validCount, products.size() - validCount);
        processingRunRepository.save(run);
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public ProcessingRunResponseDto getRun(Long runId) {
        return toResponse(processingRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Processing run not found: " + runId)));
    }

    @Transactional(readOnly = true)
    public ProcessingRunResponseDto getLatestCompletedRun() {
        return processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No completed processing run exists"));
    }

    @Transactional(readOnly = true)
    public List<ProcessingRunResponseDto> getCompletedRuns() {
        return processingRunRepository.findAllByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UploadBatch getBatch(Long id, SourceType expectedType) {
        UploadBatch batch = uploadBatchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Upload batch not found: " + id));
        if (batch.getSourceType() != expectedType) {
            throw new BadRequestException("Batch " + id + " is not a " + expectedType + " batch");
        }
        if (batch.getStatus() != UploadStatus.COMPLETED) {
            throw new BadRequestException("Batch " + id + " is not ready for processing");
        }
        return batch;
    }

    private ProcessingRunResponseDto toResponse(ProcessingRun run) {
        return new ProcessingRunResponseDto(
                run.getId(), run.getPrimarySource(), run.isFallbackMissingValues(), run.getStatus(),
                run.getStartedAt(), run.getFinishedAt(),
                (run.getValidCount() == null ? 0 : run.getValidCount()) + (run.getInvalidCount() == null ? 0 : run.getInvalidCount()),
                run.getValidCount() == null ? 0 : run.getValidCount(),
                run.getInvalidCount() == null ? 0 : run.getInvalidCount(),
                run.getCsvBatch().getOriginalFilename(),
                run.getJsonBatch().getOriginalFilename());
    }
}
