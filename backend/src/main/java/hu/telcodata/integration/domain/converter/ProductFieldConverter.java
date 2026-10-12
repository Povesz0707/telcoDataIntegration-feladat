package hu.telcodata.integration.domain.converter;

import hu.telcodata.integration.domain.enums.ProductField;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductFieldConverter implements AttributeConverter<ProductField, String> {

    @Override
    public String convertToDatabaseColumn(ProductField attribute) {
        return attribute == null ? null : attribute.propertyName();
    }

    @Override
    public ProductField convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return ProductField.findByPropertyName(value)
                .orElseGet(() -> ProductField.valueOf(value));
    }
}
