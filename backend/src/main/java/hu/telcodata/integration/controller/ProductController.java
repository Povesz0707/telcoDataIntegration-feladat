package hu.telcodata.integration.controller;

import hu.telcodata.integration.api.ProductResponseDto;
import hu.telcodata.integration.api.ProductPageResponseDto;
import hu.telcodata.integration.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Normalizált termékek", description = "A normalizálási futások termékeredményeinek lekérdezése")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Normalizált termékek lekérdezése")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A normalizált termékek listája"),
            @ApiResponse(responseCode = "404", description = "A megadott feldolgozási futás nem található")
    })
    public ResponseEntity<List<ProductResponseDto>> findProducts(
            @Parameter(description = "A feldolgozási futás azonosítója; ha nincs megadva, a legutóbbi sikeres futás kerül lekérdezésre",
                    in = ParameterIn.QUERY)
            @RequestParam(required = false) Long runId,
            @Parameter(description = "Szabad szöveges szűrő a termékmezőkre", in = ParameterIn.QUERY)
            @RequestParam(required = false, defaultValue = "") String filter,
            @Parameter(description = "Rendezés formátuma: mező:asc vagy mező:desc", in = ParameterIn.QUERY,
                    example = "finalPriceHuf:asc")
            @RequestParam(required = false) String sort,
            @Parameter(description = "Csak a valid rekordok visszaadása", in = ParameterIn.QUERY)
            @RequestParam(defaultValue = "false") boolean onlyValid) {
        return ResponseEntity.ok(productService.findProducts(runId, filter, sort, onlyValid));
    }

    @GetMapping("/page")
    @Operation(summary = "Normalizált termékek lapozott lekérdezése adatbázisoldali szűréssel és rendezéssel")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A szűrt és lapozott terméklista"),
            @ApiResponse(responseCode = "400", description = "Érvénytelen lapozási vagy rendezési paraméter"),
            @ApiResponse(responseCode = "404", description = "Nincs sikeres feldolgozási futás")
    })
    public ResponseEntity<ProductPageResponseDto> findProductsPage(
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false, defaultValue = "") String filter,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String finalPriceHuf,
            @RequestParam(required = false) String stock,
            @RequestParam(required = false) String ean,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean valid,
            @RequestParam(defaultValue = "false") boolean onlyValid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "sku:asc") String sort) {
        return ResponseEntity.ok(productService.findProductsPage(runId, filter, sku, name, manufacturer,
                finalPriceHuf, stock, ean, source, valid, onlyValid, page, size, sort));
    }
}
