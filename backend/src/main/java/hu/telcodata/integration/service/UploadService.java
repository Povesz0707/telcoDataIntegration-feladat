package hu.telcodata.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderHeaderAware;
import hu.telcodata.integration.api.UploadResponseDto;
import hu.telcodata.integration.api.UploadSourcesResponseDto;
import hu.telcodata.integration.constant.MessageConstants;
import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.enums.SourceType;
import hu.telcodata.integration.domain.UploadBatch;
import hu.telcodata.integration.domain.enums.UploadStatus;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.mapper.SourceMapper;
import hu.telcodata.integration.repository.CsvProductSourceRepository;
import hu.telcodata.integration.repository.JsonProductSourceRepository;
import hu.telcodata.integration.repository.UploadBatchRepository;
import hu.telcodata.integration.source.CsvProductInputDto;
import hu.telcodata.integration.source.JsonProductInputDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadService {

    private final ObjectMapper objectMapper;
    private final SourceMapper sourceMapper;
    private final UploadBatchRepository uploadBatchRepository;
    private final CsvProductSourceRepository csvProductSourceRepository;
    private final JsonProductSourceRepository jsonProductSourceRepository;

    @Transactional
    public UploadSourcesResponseDto uploadSources(MultipartFile csvFile, MultipartFile jsonFile) {
        log.info("Source upload started: csvFile={}, jsonFile={}",
                csvFile == null ? null : csvFile.getOriginalFilename(),
                jsonFile == null ? null : jsonFile.getOriginalFilename());
        byte[] csvContent = readContent(csvFile);
        List<CsvProductInputDto> csvInputs = parseCsv(csvContent);
        byte[] jsonContent = readContent(jsonFile);
        List<JsonProductInputDto> jsonInputs = parseJson(jsonContent);

        UploadBatch csvBatch = storeCsv(csvFile, csvContent, csvInputs);
        UploadBatch jsonBatch = storeJson(jsonFile, jsonContent, jsonInputs);
        log.info("Source upload completed: csvBatchId={}, csvRows={}, jsonBatchId={}, jsonRows={}",
                csvBatch.getId(), csvInputs.size(), jsonBatch.getId(), jsonInputs.size());
        return new UploadSourcesResponseDto(toResponse(csvBatch), toResponse(jsonBatch));
    }

    @Transactional
    public UploadResponseDto uploadCsv(MultipartFile file) {
        log.info("CSV upload started: file={}", file == null ? null : file.getOriginalFilename());
        byte[] content = readContent(file);
        List<CsvProductInputDto> inputs = parseCsv(content);
        UploadBatch batch = storeCsv(file, content, inputs);
        log.info("CSV upload completed: batchId={}, rows={}", batch.getId(), inputs.size());
        return toResponse(batch);
    }

    @Transactional
    public UploadResponseDto uploadJson(MultipartFile file) {
        log.info("JSON upload started: file={}", file == null ? null : file.getOriginalFilename());
        byte[] content = readContent(file);
        List<JsonProductInputDto> inputs = parseJson(content);
        UploadBatch batch = storeJson(file, content, inputs);
        log.info("JSON upload completed: batchId={}, rows={}", batch.getId(), inputs.size());
        return toResponse(batch);
    }

    private UploadBatch storeCsv(MultipartFile file, byte[] content, List<CsvProductInputDto> inputs) {
        UploadBatch batch = new UploadBatch(SourceType.CSV, file.getOriginalFilename(), checksum(content),
                Instant.now(), UploadStatus.COMPLETED, inputs.size());
        uploadBatchRepository.save(batch);

        List<CsvProductSource> entities = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            CsvProductSource entity = sourceMapper.toCsvEntity(inputs.get(i));
            entity.attachTo(batch, i + 2, inputs.get(i).rawData());
            entities.add(entity);
        }
        csvProductSourceRepository.saveAll(entities);
        return batch;
    }

    private UploadBatch storeJson(MultipartFile file, byte[] content, List<JsonProductInputDto> inputs) {
        UploadBatch batch = new UploadBatch(SourceType.JSON, file.getOriginalFilename(), checksum(content),
                Instant.now(), UploadStatus.COMPLETED, inputs.size());
        uploadBatchRepository.save(batch);

        List<JsonProductSource> entities = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            JsonProductSource entity = sourceMapper.toJsonEntity(inputs.get(i));
            entity.attachTo(batch, i + 1, rawJson(inputs.get(i)));
            entities.add(entity);
        }
        jsonProductSourceRepository.saveAll(entities);
        return batch;
    }

    private List<CsvProductInputDto> parseCsv(byte[] content) {
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             CSVReaderHeaderAware csvReader = new CSVReaderHeaderAware(reader)) {
            List<CsvProductInputDto> result = new ArrayList<>();
            Map<String, String> row;
            while ((row = csvReader.readMap()) != null) {
                result.add(new CsvProductInputDto(
                        trim(row.get("sku")),
                        trim(row.get("product_name")),
                        parseDecimal(row.get("gross_price_huf"), "gross_price_huf"),
                        parseInteger(row.get("stock_qty"), "stock_qty"),
                        trim(row.get("brand")),
                        row.toString()));
            }
            if (result.isEmpty()) {
                throw new BadRequestException(MessageConstants.CSV_EMPTY);
            }
            return result;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException(MessageConstants.CSV_PARSE_ERROR, exception);
        }
    }

    private List<JsonProductInputDto> parseJson(byte[] content) {
        try {
            List<JsonProductInputDto> result = objectMapper.readValue(content, new TypeReference<>() {
            });
            if (result == null || result.isEmpty()) {
                throw new BadRequestException(MessageConstants.JSON_EMPTY);
            }
            return result;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException(MessageConstants.JSON_PARSE_ERROR, exception);
        }
    }

    private byte[] readContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(MessageConstants.INPUT_FILE_REQUIRED);
        }
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BadRequestException(MessageConstants.INPUT_FILE_READ_ERROR, exception);
        }
    }

    private BigDecimal parseDecimal(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(" ", "").trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException(MessageConstants.invalidNumericValue(field, value), exception);
        }
    }

    private Integer parseInteger(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException(MessageConstants.invalidIntegerValue(field, value), exception);
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String rawJson(JsonProductInputDto input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception exception) {
            return input.toString();
        }
    }

    private String checksum(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(MessageConstants.SHA_256_UNAVAILABLE, exception);
        }
    }

    private UploadResponseDto toResponse(UploadBatch batch) {
        return new UploadResponseDto(batch.getId(), batch.getSourceType(), batch.getOriginalFilename(),
                batch.getRowCount(), batch.getUploadedAt());
    }
}
