package hu.telcodata.integration.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JsonProductInputDto(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("netPrice") BigDecimal netPrice,
        @JsonProperty("currency") String currency,
        @JsonProperty("vatRate") BigDecimal vatRate,
        @JsonProperty("quantityAvailable") Integer quantityAvailable,
        @JsonProperty("manufacturer") String manufacturer,
        @JsonProperty("ean") String ean,
        @JsonProperty("updatedAt") OffsetDateTime updatedAt
) {
}
