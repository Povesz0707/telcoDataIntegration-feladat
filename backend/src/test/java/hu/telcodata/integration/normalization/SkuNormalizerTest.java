package hu.telcodata.integration.normalization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkuNormalizerTest {

    private final SkuNormalizer normalizer = new SkuNormalizer();

    @Test
    void normalizesDifferentSourceSkuFormatsToTheSameKey() {
        assertThat(normalizer.normalize("P-1001")).isEqualTo("1001");
        assertThat(normalizer.normalize("SKU1001")).isEqualTo("1001");
        assertThat(normalizer.normalize("SKU-1001")).isEqualTo("1001");
        assertThat(normalizer.normalize("P_1001")).isEqualTo("1001");
    }

    @Test
    void normalizesManufacturerAliasesForComparison() {
        assertThat(normalizer.normalizeManufacturer("Wire&Co"))
                .isEqualTo(normalizer.normalizeManufacturer("WIRECO"));
        assertThat(normalizer.normalizeManufacturer("FixIt"))
                .isEqualTo(normalizer.normalizeManufacturer("Fix-It"));
    }
}
