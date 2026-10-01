package br.com.senai.sublime_app.pricing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.pricing.dto.TechniquePriceTableResponseDTO;
import br.com.senai.sublime_app.pricing.dto.TechniqueResponseDTO;
import br.com.senai.sublime_app.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/techniques")
@Tag(name = "Técnicas e preços", description = """
        Consulta de técnicas e das tabelas de preço vigentes.
        Fluxo da tela de contrato: listar técnicas → escolher uma → buscar os preços do grupo dela.
        """)
public class TechniqueController {

    private final PricingService pricingService;

    public TechniqueController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Operation(summary = "Listar técnicas ativas", description = """
            Retorna as técnicas ativas com o grupo de preço e o `pricingModel` de cada uma.
            O `pricingModel` indica se a tabela de preço da técnica é por duração
            (`DURATION_BASED`) ou por frequência semanal (`FREQUENCY_BASED`).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = TechniqueResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<TechniqueResponseDTO>> findActive() {
        return ResponseEntity.ok(pricingService.findActiveTechniques());
    }

    @Operation(summary = "Tabela de preços vigente da técnica", description = """
            Retorna as linhas de preço vigentes (`validTo` nulo) do grupo da técnica,
            buscadas na tabela indicada pelo `pricingModel` do grupo.

            Cada linha preenche só um dos eixos:
            - `durationMinutes` → grupos `DURATION_BASED` (usar o id em `sessionDurationPriceId` no contrato)
            - `weeklyFrequency` → grupos `FREQUENCY_BASED` (usar o id em `sessionFrequencyPriceId` no contrato)

            Técnicas do mesmo grupo retornam a mesma lista de preços.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tabela retornada com sucesso", content = @Content(schema = @Schema(implementation = TechniquePriceTableResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Técnica não encontrada", content = @Content)
    })
    @GetMapping("/{id}/prices")
    public ResponseEntity<TechniquePriceTableResponseDTO> findCurrentPriceTable(
            @Parameter(description = "Identificador da técnica", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pricingService.findCurrentPriceTable(id));
    }
}
