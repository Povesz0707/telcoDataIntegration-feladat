package hu.telcodata.integration.controller;

import hu.telcodata.integration.api.UploadResponseDto;
import hu.telcodata.integration.api.UploadSourcesResponseDto;
import hu.telcodata.integration.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
@Tag(name = "Adatforrások", description = "CSV és JSON adatforrások feltöltése")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(value = "/sources", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CSV és JSON adatforrások atomikus feltöltése")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mindkét fájl sikeresen eltárolva",
                    content = @Content(schema = @Schema(implementation = UploadSourcesResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Érvénytelen vagy üres fájl")
    })
    public ResponseEntity<UploadSourcesResponseDto> uploadSources(
            @RequestPart("csvFile") MultipartFile csvFile,
            @RequestPart("jsonFile") MultipartFile jsonFile) {
        return ResponseEntity.ok(uploadService.uploadSources(csvFile, jsonFile));
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CSV adatforrás feltöltése")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A fájl sikeresen eltárolva",
                    content = @Content(schema = @Schema(implementation = UploadResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Érvénytelen vagy üres fájl")
    })
    public ResponseEntity<UploadResponseDto> uploadCsv(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(uploadService.uploadCsv(file));
    }

    @PostMapping(value = "/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "JSON adatforrás feltöltése")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A fájl sikeresen eltárolva",
                    content = @Content(schema = @Schema(implementation = UploadResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Érvénytelen vagy üres fájl")
    })
    public ResponseEntity<UploadResponseDto> uploadJson(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(uploadService.uploadJson(file));
    }
}
