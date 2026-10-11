package hu.telcodata.integration.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReaderHeaderAware;
import hu.telcodata.integration.api.UploadResponseDto;
import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.domain.SourceType;
import hu.telcodata.integration.domain.UploadBatch;
import hu.telcodata.integration.domain.UploadStatus;
import hu.telcodata.integration.exception.BadRequestException;
import hu.telcodata.integration.mapper.SourceMapper;
import hu.telcodata.integration.repository.CsvProductSourceRepository;
import hu.telcodata.integration.repository.JsonProductSourceRepository;
import hu.telcodata.integration.repository.UploadBatchRepository;
import hu.telcodata.integration.source.CsvProductInputDto;
import hu.telcodata.integration.source.JsonProductInputDto;
import lombok.RequiredArgsConstructor;
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
public class UploadService {

    private final ObjectMapper objectMapper;
    private final SourceMapper sourceMapper;
    private final UploadBatchRepository uploadBatchRepository;
    private final CsvProductSourceRepository csvProductSourceRepository;
    private final JsonProductSourceRepository jsonProductSourceRepository;

    @Transactional
    public UploadResponseDto uploadCsv(MultipartFile file) {
        byte[] content = readContent(file);
        List<CsvProductInputDto> inputs = parseCsv(content);
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
        return toResponse(batch);
    }

    @Transactional
    public UploadResponseDto uploadJson(MultipartFile file) {
        byte[] content = readContent(file);
        List<JsonProductInputDto> inputs = parseJson(content);
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
        return toResponse(batch);
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
                throw new BadRequestException("The CSV file contains no data rows");
            }
            return result;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("The CSV file could not be parsed", exception);
        }
    }

    private List<JsonProductInputDto> parseJson(byte[] content) {
        try {
            List<JsonProductInputDto> result = objectMapper.readValue(content, new TypeReference<>() {
            });
            if (result == null || result.isEmpty()) {
                throw new BadRequestException("The JSON file contains no records");
            }
            return result;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("The JSON file could not be parsed", exception);
        }
    }

    private byte[] readContent(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("An input file is required");
        }
        try {
            return file.getBytes();
        } catch (Exception exception) {
            throw new BadRequestException("The input file could not be read", exception);
        }
    }

    private BigDecimal parseDecimal(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(" ", "").trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid numeric value in " + field + ": " + value, exception);
        }
    }

    private Integer parseInteger(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid integer value in " + field + ": " + value, exception);
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
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private UploadResponseDto toResponse(UploadBatch batch) {
        return new UploadResponseDto(batch.getId(), batch.getSourceType(), batch.getOriginalFilename(),
                batch.getRowCount(), batch.getUploadedAt());
    }
}
