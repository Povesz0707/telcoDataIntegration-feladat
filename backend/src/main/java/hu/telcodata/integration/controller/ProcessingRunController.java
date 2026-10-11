package hu.telcodata.integration.controller;

import hu.telcodata.integration.api.ProcessingRunRequestDto;
import hu.telcodata.integration.api.ProcessingRunResponseDto;
import hu.telcodata.integration.service.ProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processing-runs")
@RequiredArgsConstructor
@Tag(name = "Feldolgozások", description = "Adatforrások normalizálása és feldolgozási előzmények")
public class ProcessingRunController {

    private final ProcessingService processingService;

    @PostMapping
    @Operation(summary = "Normalizálási feldolgozás indítása")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A feldolgozás eredménye",
                    content = @Content(schema = @Schema(implementation = ProcessingRunResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Érvénytelen feldolgozási kérés")
    })
    public ResponseEntity<ProcessingRunResponseDto> process(@Valid @RequestBody ProcessingRunRequestDto request) {
        return ResponseEntity.ok(processingService.process(request));
    }

    @GetMapping
    @Operation(summary = "Befejezett feldolgozások listázása")
    @ApiResponse(responseCode = "200", description = "A feldolgozási előzmények listája")
    public ResponseEntity<List<ProcessingRunResponseDto>> getCompletedRuns() {
        return ResponseEntity.ok(processingService.getCompletedRuns());
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Feldolgozás lekérdezése azonosító alapján")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A feldolgozási futás",
                    content = @Content(schema = @Schema(implementation = ProcessingRunResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "A feldolgozási futás nem található")
    })
    public ResponseEntity<ProcessingRunResponseDto> getRun(
            @Parameter(description = "A feldolgozási futás azonosítója", required = true, example = "1")
            @PathVariable Long runId) {
        return ResponseEntity.ok(processingService.getRun(runId));
    }

    @GetMapping("/latest")
    @Operation(summary = "Legutóbbi befejezett feldolgozás lekérdezése")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A legutóbbi feldolgozási futás",
                    content = @Content(schema = @Schema(implementation = ProcessingRunResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Még nincs befejezett feldolgozás")
    })
    public ResponseEntity<ProcessingRunResponseDto> getLatest() {
        return ResponseEntity.ok(processingService.getLatestCompletedRun());
    }
}
