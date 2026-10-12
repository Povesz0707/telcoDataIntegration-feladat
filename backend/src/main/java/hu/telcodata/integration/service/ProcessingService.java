package hu.telcodata.integration.service;

import hu.telcodata.integration.api.ProcessingRunRequestDto;
import hu.telcodata.integration.api.ProcessingRunResponseDto;
import hu.telcodata.integration.constant.MessageConstants;
import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.ProcessingRun;
import hu.telcodata.integration.domain.enums.ProcessingStatus;
import hu.telcodata.integration.domain.enums.SourceType;
import hu.telcodata.integration.domain.UploadBatch;
import hu.telcodata.integration.domain.enums.UploadStatus;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.exception.ResourceNotFoundException;
import hu.telcodata.integration.normalization.ProductMergeService;
import hu.telcodata.integration.repository.CsvProductSourceRepository;
import hu.telcodata.integration.repository.JsonProductSourceRepository;
import hu.telcodata.integration.repository.ProcessingRunRepository;
import hu.telcodata.integration.repository.UploadBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingService {

    private final UploadBatchRepository uploadBatchRepository;
    private final CsvProductSourceRepository csvProductSourceRepository;
    private final JsonProductSourceRepository jsonProductSourceRepository;
    private final ProcessingRunRepository processingRunRepository;
    private final ProductMergeService productMergeService;

    @Transactional
    public ProcessingRunResponseDto process(ProcessingRunRequestDto request) {
        log.info("Processing started: csvBatchId={}, jsonBatchId={}, primarySource={}, fallbackMissingValues={}",
                request.csvBatchId(), request.jsonBatchId(), request.primarySource(), request.fallbackMissingValues());
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
        int invalidCount = products.size() - validCount;
        if (invalidCount > 0) {
            log.warn("Processing completed with invalid products: runId={}, totalCount={}, validCount={}, invalidCount={}",
                    run.getId(), products.size(), validCount, invalidCount);
        } else {
            log.info("Processing completed: runId={}, totalCount={}, validCount={}",
                    run.getId(), products.size(), validCount);
        }
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public ProcessingRunResponseDto getRun(Long runId) {
        return toResponse(processingRunRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.processingRunNotFound(runId))));
    }

    @Transactional(readOnly = true)
    public ProcessingRunResponseDto getLatestCompletedRun() {
        return processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.NO_COMPLETED_PROCESSING_RUN));
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
                .orElseThrow(() -> new ResourceNotFoundException(MessageConstants.uploadBatchNotFound(id)));
        if (batch.getSourceType() != expectedType) {
            throw new BadRequestException(MessageConstants.batchWrongSource(id, expectedType));
        }
        if (batch.getStatus() != UploadStatus.COMPLETED) {
            throw new BadRequestException(MessageConstants.BATCH_NOT_READY.formatted(id));
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
