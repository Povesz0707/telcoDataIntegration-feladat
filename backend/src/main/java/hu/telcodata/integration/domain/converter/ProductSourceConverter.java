package hu.telcodata.integration.domain.converter;

import hu.telcodata.integration.domain.enums.ProductSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductSourceConverter implements AttributeConverter<ProductSource, String> {

    @Override
    public String convertToDatabaseColumn(ProductSource attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public ProductSource convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return ProductSource.findByValue(value)
                .orElseGet(() -> ProductSource.valueOf(value));
    }
}
