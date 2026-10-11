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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessingServiceTest {

    @Mock
    private UploadBatchRepository uploadBatchRepository;

    @Mock
    private CsvProductSourceRepository csvProductSourceRepository;

    @Mock
    private JsonProductSourceRepository jsonProductSourceRepository;

    @Mock
    private ProcessingRunRepository processingRunRepository;

    @Mock
    private ProductMergeService productMergeService;

    private ProcessingService service;
    private UploadBatch csvBatch;
    private UploadBatch jsonBatch;

    @BeforeEach
    void setUp() {
        service = new ProcessingService(uploadBatchRepository, csvProductSourceRepository,
                jsonProductSourceRepository, processingRunRepository, productMergeService);
    }

    @Test
    void processMergesSourcesCompletesRunAndCalculatesCounters() {
        stubCompletedBatches();
        NormalizedProduct valid = product("SKU-1", true);
        NormalizedProduct invalid = product("SKU-2", false);
        when(csvProductSourceRepository.findAllByUploadBatchIdOrderByRowNumber(1L)).thenReturn(List.of());
        when(jsonProductSourceRepository.findAllByUploadBatchIdOrderByRowNumber(2L)).thenReturn(List.of());
        when(processingRunRepository.save(any(ProcessingRun.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productMergeService.merge(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(valid, invalid));

        ProcessingRunResponseDto response = service.process(new ProcessingRunRequestDto(1L, 2L,
                SourceType.CSV, true));

        assertThat(response.status()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.validCount()).isOne();
        assertThat(response.invalidCount()).isOne();
        assertThat(response.csvFilename()).isEqualTo("products.csv");
        assertThat(response.jsonFilename()).isEqualTo("products.json");
        verify(productMergeService).merge(List.of(), List.of(), SourceType.CSV, true);
        verify(processingRunRepository, org.mockito.Mockito.times(2)).save(any(ProcessingRun.class));
    }

    @Test
    void processRejectsMissingCsvBatch() {
        when(uploadBatchRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.process(new ProcessingRunRequestDto(1L, 2L,
                SourceType.CSV, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Upload batch not found: 1");
        verify(productMergeService, never()).merge(any(), any(), any(), anyBoolean());
    }

    @Test
    void processRejectsBatchWithWrongSourceType() {
        UploadBatch wrongBatch = mock(UploadBatch.class);
        when(wrongBatch.getSourceType()).thenReturn(SourceType.JSON);
        when(uploadBatchRepository.findById(1L)).thenReturn(Optional.of(wrongBatch));

        assertThatThrownBy(() -> service.process(new ProcessingRunRequestDto(1L, 2L,
                SourceType.CSV, false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Batch 1 is not a CSV batch");
    }

    @Test
    void processRejectsBatchThatIsNotCompleted() {
        UploadBatch incompleteBatch = mock(UploadBatch.class);
        when(incompleteBatch.getSourceType()).thenReturn(SourceType.CSV);
        when(incompleteBatch.getStatus()).thenReturn(UploadStatus.FAILED);
        when(uploadBatchRepository.findById(1L)).thenReturn(Optional.of(incompleteBatch));

        assertThatThrownBy(() -> service.process(new ProcessingRunRequestDto(1L, 2L,
                SourceType.CSV, false)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Batch 1 is not ready for processing");
    }

    @Test
    void getRunReturnsExistingRun() {
        ProcessingRun run = processingRun();
        when(processingRunRepository.findById(3L)).thenReturn(Optional.of(run));

        ProcessingRunResponseDto response = service.getRun(3L);

        assertThat(response.status()).isEqualTo(ProcessingStatus.COMPLETED);
        assertThat(response.totalCount()).isZero();
        assertThat(response.csvFilename()).isEqualTo("products.csv");
    }

    @Test
    void getRunRejectsUnknownRun() {
        when(processingRunRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRun(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Processing run not found: 99");
    }

    @Test
    void getLatestCompletedRunRejectsMissingCompletedRun() {
        when(processingRunRepository.findTopByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestCompletedRun())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No completed processing run exists");
    }

    @Test
    void getCompletedRunsMapsCompletedRuns() {
        ProcessingRun run = processingRun();
        when(processingRunRepository.findAllByStatusOrderByStartedAtDesc(ProcessingStatus.COMPLETED))
                .thenReturn(List.of(run));

        assertThat(service.getCompletedRuns()).hasSize(1)
                .first()
                .extracting(ProcessingRunResponseDto::status)
                .isEqualTo(ProcessingStatus.COMPLETED);
    }

    private void stubCompletedBatches() {
        csvBatch = batch(1L, SourceType.CSV, "products.csv", UploadStatus.COMPLETED);
        jsonBatch = batch(2L, SourceType.JSON, "products.json", UploadStatus.COMPLETED);
        when(uploadBatchRepository.findById(1L)).thenReturn(Optional.of(csvBatch));
        when(uploadBatchRepository.findById(2L)).thenReturn(Optional.of(jsonBatch));
    }

    private UploadBatch batch(Long id, SourceType sourceType, String filename, UploadStatus status) {
        UploadBatch batch = mock(UploadBatch.class);
        when(batch.getId()).thenReturn(id);
        when(batch.getSourceType()).thenReturn(sourceType);
        when(batch.getOriginalFilename()).thenReturn(filename);
        when(batch.getStatus()).thenReturn(status);
        return batch;
    }

    private ProcessingRun processingRun() {
        csvBatch = responseBatch("products.csv");
        jsonBatch = responseBatch("products.json");
        ProcessingRun run = new ProcessingRun(csvBatch, jsonBatch, SourceType.CSV, false);
        run.complete(0, 0);
        return run;
    }

    private UploadBatch responseBatch(String filename) {
        UploadBatch batch = mock(UploadBatch.class);
        when(batch.getOriginalFilename()).thenReturn(filename);
        return batch;
    }

    private NormalizedProduct product(String sku, boolean valid) {
        NormalizedProduct product = new NormalizedProduct(sku, "Product", "ACME",
                BigDecimal.valueOf(100), 1, "EAN-1", Instant.now(), "CSV", "CSV");
        product.markValid(valid);
        return product;
    }
}
