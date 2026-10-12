package hu.telcodata.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hu.telcodata.integration.api.UploadResponseDto;
import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.enums.SourceType;
import hu.telcodata.integration.domain.UploadBatch;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.mapper.SourceMapper;
import hu.telcodata.integration.repository.CsvProductSourceRepository;
import hu.telcodata.integration.repository.JsonProductSourceRepository;
import hu.telcodata.integration.repository.UploadBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock
    private SourceMapper sourceMapper;

    @Mock
    private UploadBatchRepository uploadBatchRepository;

    @Mock
    private CsvProductSourceRepository csvProductSourceRepository;

    @Mock
    private JsonProductSourceRepository jsonProductSourceRepository;

    private UploadService service;

    @BeforeEach
    void setUp() {
        service = new UploadService(new ObjectMapper().registerModule(new JavaTimeModule()), sourceMapper,
                uploadBatchRepository, csvProductSourceRepository, jsonProductSourceRepository);
    }

    @Test
    void uploadCsvParsesRowsTrimsValuesAndStoresSourceRecords() {
        byte[] content = "sku,product_name,gross_price_huf,stock_qty,brand\n SKU-1 , Widget , 12 700 , 4 , ACME \n"
                .getBytes();
        MultipartFile file = new MockMultipartFile("file", "products.csv", "text/csv", content);
        CsvProductSource mappedEntity = new CsvProductSource(null, null, null, null, null, null, null, null);
        when(sourceMapper.toCsvEntity(any())).thenReturn(mappedEntity);
        when(uploadBatchRepository.save(any(UploadBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UploadResponseDto response = service.uploadCsv(file);

        assertThat(response.sourceType()).isEqualTo(SourceType.CSV);
        assertThat(response.originalFilename()).isEqualTo("products.csv");
        assertThat(response.rowCount()).isOne();
        assertThat(response.uploadedAt()).isNotNull();
        assertThat(mappedEntity.getRowNumber()).isEqualTo(2);
        assertThat(mappedEntity.getUploadBatch()).isNotNull();
        assertThat(mappedEntity.getRawData()).contains("SKU-1");
        verify(csvProductSourceRepository).saveAll(List.of(mappedEntity));
    }

    @Test
    void uploadJsonParsesRecordsAndStoresSourceRecords() {
        byte[] content = ("[{\"id\":\"SKU-1\",\"name\":\"Widget\",\"netPrice\":100,"
                + "\"currency\":\"HUF\",\"vatRate\":0.27,\"quantityAvailable\":3}]")
                .getBytes();
        MultipartFile file = new MockMultipartFile("file", "products.json", "application/json", content);
        JsonProductSource mappedEntity = new JsonProductSource(null, null, null, null, null, null, null,
                null, null, null, null, null);
        when(sourceMapper.toJsonEntity(any())).thenReturn(mappedEntity);
        when(uploadBatchRepository.save(any(UploadBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UploadResponseDto response = service.uploadJson(file);

        assertThat(response.sourceType()).isEqualTo(SourceType.JSON);
        assertThat(response.originalFilename()).isEqualTo("products.json");
        assertThat(response.rowCount()).isOne();
        assertThat(mappedEntity.getRowNumber()).isOne();
        assertThat(mappedEntity.getUploadBatch()).isNotNull();
        assertThat(mappedEntity.getRawData()).contains("SKU-1");
        verify(jsonProductSourceRepository).saveAll(List.of(mappedEntity));
    }

    @Test
    void uploadRejectsNullOrEmptyInputFile() {
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.uploadCsv(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An input file is required");
        assertThatThrownBy(() -> service.uploadJson(emptyFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("An input file is required");
    }

    @Test
    void uploadRejectsCsvWithoutRows() {
        MultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv",
                "sku,product_name,gross_price_huf,stock_qty,brand\n".getBytes());

        assertThatThrownBy(() -> service.uploadCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The CSV file contains no data rows");
    }

    @Test
    void uploadRejectsMalformedJson() {
        MultipartFile file = new MockMultipartFile("file", "broken.json", "application/json",
                "{broken-json".getBytes());

        assertThatThrownBy(() -> service.uploadJson(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The JSON file could not be parsed")
                .hasCauseInstanceOf(Exception.class);
    }

    @Test
    void uploadRejectsInvalidCsvNumber() {
        MultipartFile file = new MockMultipartFile("file", "invalid.csv", "text/csv",
                "sku,product_name,gross_price_huf,stock_qty,brand\nSKU-1,Widget,not-a-number,2,ACME\n"
                        .getBytes());

        assertThatThrownBy(() -> service.uploadCsv(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid numeric value in gross_price_huf");
    }

    @Test
    void uploadWrapsUnreadableFileInBadRequest() throws Exception {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        doThrow(new java.io.IOException("read failed")).when(file).getBytes();

        assertThatThrownBy(() -> service.uploadJson(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The input file could not be read");
    }
}
