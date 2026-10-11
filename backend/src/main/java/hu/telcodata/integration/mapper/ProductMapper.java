package hu.telcodata.integration.mapper;

import hu.telcodata.integration.api.ProductIssueResponseDto;
import hu.telcodata.integration.api.ProductResponseDto;
import hu.telcodata.integration.domain.NormalizedProduct;
import hu.telcodata.integration.domain.NormalizedProductIssue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponseDto toResponse(NormalizedProduct product);

    ProductIssueResponseDto toResponse(NormalizedProductIssue issue);
}
