package hu.telcodata.integration.mapper;

import hu.telcodata.integration.domain.CsvProductSource;
import hu.telcodata.integration.domain.JsonProductSource;
import hu.telcodata.integration.source.CsvProductInputDto;
import hu.telcodata.integration.source.JsonProductInputDto;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SourceMapper {

    @Mapping(target = "uploadBatch", ignore = true)
    @Mapping(target = "rowNumber", ignore = true)
    @Mapping(target = "rawData", ignore = true)
    CsvProductSource toCsvEntity(CsvProductInputDto input);

    @Mapping(target = "uploadBatch", ignore = true)
    @Mapping(target = "rowNumber", ignore = true)
    @Mapping(target = "sourceId", source = "id")
    @Mapping(target = "rawData", ignore = true)
    JsonProductSource toJsonEntity(JsonProductInputDto input);
}
