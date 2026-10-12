package hu.telcodata.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.telcodata.integration.api.UploadResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class ProductIntegrationTest {

    private static final String CSV_CONTENT = """
            sku,product_name,gross_price_huf,stock_qty,brand
            P-1001,Integration Widget,12 700,10,ACME
            P-1002,CSV Only Product,2 540,2,WireCo
            """;

    private static final String JSON_CONTENT = """
            [
              {
                "id": "SKU1001",
                "name": "Integration Widget",
                "netPrice": 10000,
                "currency": "HUF",
                "vatRate": 0.27,
                "quantityAvailable": null,
                "manufacturer": "ACME",
                "ean": "599000000001",
                "updatedAt": "2025-10-01T10:00:00Z"
              },
              {
                "id": "SKU-9999",
                "name": "Invalid Product",
                "netPrice": null,
                "currency": "HUF",
                "vatRate": 0.27,
                "quantityAvailable": 1,
                "manufacturer": null,
                "ean": null,
                "updatedAt": "2025-10-02T10:00:00Z"
              }
            ]
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE normalized_product_issue, normalized_product, "
                + "processing_run, csv_product_source, json_product_source, upload_batch "
                + "RESTART IDENTITY CASCADE");
    }

    @Test
    void uploadsProcessesAndQueriesNormalizedProductsThroughHttp() throws Exception {
        long csvBatchId = uploadCsv();
        long jsonBatchId = uploadJson();

        String processRequest = """
                {
                  "csvBatchId": %d,
                  "jsonBatchId": %d,
                  "primarySource": "JSON",
                  "fallbackMissingValues": true
                }
                """.formatted(csvBatchId, jsonBatchId);

        String processResponse = mockMvc.perform(post("/api/processing-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(processRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.validCount").value(2))
                .andExpect(jsonPath("$.invalidCount").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long runId = objectMapper.readTree(processResponse).get("runId").asLong();

        mockMvc.perform(get("/api/products")
                        .param("runId", String.valueOf(runId))
                        .param("filter", "1001")
                        .param("sort", "finalPriceHuf:asc")
                        .param("onlyValid", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku").value("SKU1001"))
                .andExpect(jsonPath("$[0].source").value("CSV+JSON"))
                .andExpect(jsonPath("$[0].stock").value(10))
                .andExpect(jsonPath("$[0].valid").value(true))
                .andExpect(jsonPath("$[0].issues[*].code").value(hasItem("FALLBACK_VALUE_USED")))
                .andExpect(jsonPath("$[0].issues[*].field").value(hasItem("stock")));

        mockMvc.perform(get("/api/products")
                        .param("filter", "")
                        .param("sort", "")
                        .param("onlyValid", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        mockMvc.perform(get("/api/products/page")
                        .param("runId", String.valueOf(runId))
                        .param("sku", "SKU1001")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "sku:asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value("SKU1001"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        mockMvc.perform(get("/api/products/page")
                        .param("runId", String.valueOf(runId))
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Page must not be negative"));
    }

    @Test
    void rejectsInvalidUploadAndMissingLatestRun() throws Exception {
        mockMvc.perform(multipart("/api/uploads/json")
                        .file(new MockMultipartFile("file", "invalid.json", MediaType.APPLICATION_JSON_VALUE,
                                "not-json".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("The JSON file could not be parsed"));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("No completed processing run exists"));

    }

    @Test
    void rejectsProcessingWhenBatchBelongsToWrongSource() throws Exception {
        long csvBatchId = uploadCsv();
        String processRequest = """
                {
                  "csvBatchId": %d,
                  "jsonBatchId": %d,
                  "primarySource": "CSV",
                  "fallbackMissingValues": false
                }
                """.formatted(csvBatchId, csvBatchId);

        mockMvc.perform(post("/api/processing-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(processRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Batch " + csvBatchId + " is not a JSON batch"));
    }

    @Test
    void mapsMalformedRequestsToClientErrors() throws Exception {
        mockMvc.perform(post("/api/processing-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The request body is missing or malformed"));

        mockMvc.perform(multipart("/api/uploads/csv"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required request part is missing: file"));

        mockMvc.perform(get("/api/products/page").param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for request parameter page: not-a-number"));
    }

    @Test
    void atomicallyRejectsSourceUploadWhenTheSecondFileIsInvalid() throws Exception {
        mockMvc.perform(multipart("/api/uploads/sources")
                        .file(new MockMultipartFile("csvFile", "products.csv", "text/csv",
                                CSV_CONTENT.getBytes(StandardCharsets.UTF_8)))
                        .file(new MockMultipartFile("jsonFile", "broken.json", MediaType.APPLICATION_JSON_VALUE,
                                "{broken-json".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The JSON file could not be parsed"));

        Integer uploadCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM upload_batch", Integer.class);
        assertThat(uploadCount).isZero();
    }

    private long uploadCsv() throws Exception {
        String response = mockMvc.perform(multipart("/api/uploads/csv")
                        .file(new MockMultipartFile("file", "integration.csv", "text/csv",
                                CSV_CONTENT.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("CSV"))
                .andExpect(jsonPath("$.rowCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, UploadResponseDto.class).batchId();
    }

    private long uploadJson() throws Exception {
        String response = mockMvc.perform(multipart("/api/uploads/json")
                        .file(new MockMultipartFile("file", "integration.json", MediaType.APPLICATION_JSON_VALUE,
                                JSON_CONTENT.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("JSON"))
                .andExpect(jsonPath("$.rowCount").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, UploadResponseDto.class).batchId();
    }
}
