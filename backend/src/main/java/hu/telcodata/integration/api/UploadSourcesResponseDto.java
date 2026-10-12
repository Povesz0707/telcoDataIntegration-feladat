package hu.telcodata.integration.api;

public record UploadSourcesResponseDto(
        UploadResponseDto csv,
        UploadResponseDto json
) {
}
