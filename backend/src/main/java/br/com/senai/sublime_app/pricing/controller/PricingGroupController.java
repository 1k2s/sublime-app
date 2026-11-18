package br.com.senai.sublime_app.pricing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.pricing.dto.PricingGroupPriceTableResponseDTO;
import br.com.senai.sublime_app.pricing.dto.PricingGroupRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PricingGroupResponseDTO;
import br.com.senai.sublime_app.pricing.service.PricingGroupService;
import br.com.senai.sublime_app.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pricing-groups")
@Tag(name = "Grupos de preço", description = """
        Grupos de preço (técnicas do mesmo grupo têm a mesma tabela). Regras: "PricingGroup"
        no `docs/domain-model.md`.
        """)
public class PricingGroupController {

    private final PricingGroupService pricingGroupService;
    private final PricingService pricingService;

    public PricingGroupController(PricingGroupService pricingGroupService, PricingService pricingService) {
        this.pricingGroupService = pricingGroupService;
        this.pricingService = pricingService;
    }

    @Operation(summary = "Cadastrar grupo de preço", description = """
            `pricingModel` (`DURATION_BASED` ou `FREQUENCY_BASED`) define a tabela e não muda
            depois.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grupo criado", content = @Content(schema = @Schema(implementation = PricingGroupResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe um grupo com esse nome", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PricingGroupResponseDTO> create(@Valid @RequestBody PricingGroupRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingGroupService.create(dto));
    }

    @Operation(summary = "Listar grupos de preço", description = "Retorna todos os grupos de precificação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = PricingGroupResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<PricingGroupResponseDTO>> findAll() {
        return ResponseEntity.ok(pricingGroupService.findAll());
    }

    @Operation(summary = "Tabela de preços vigente do grupo", description = """
            Linhas vigentes do grupo (`validTo` nulo), mesmo sem técnica ativa no grupo: o preço
            pertence ao grupo. Para o preço de um contrato a partir da técnica, use
            `GET /api/techniques/{id}/prices`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tabela retornada com sucesso", content = @Content(schema = @Schema(implementation = PricingGroupPriceTableResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Grupo de preço não encontrado", content = @Content)
    })
    @GetMapping("/{id}/prices")
    public ResponseEntity<PricingGroupPriceTableResponseDTO> findCurrentPriceTable(
            @Parameter(description = "Identificador do grupo de preço", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pricingService.findCurrentPriceTableByGroup(id));
    }

    @Operation(summary = "Histórico de preços do grupo", description = """
            Todas as linhas do grupo, da mais recente para a mais antiga: as vigentes (`validTo`
            nulo) e as encerradas por reajuste (`validTo` preenchido, exclusivo: a linha valeu até
            o dia anterior). Usado pela tela de preços.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso", content = @Content(schema = @Schema(implementation = PricingGroupPriceTableResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Grupo de preço não encontrado", content = @Content)
    })
    @GetMapping("/{id}/prices/history")
    public ResponseEntity<PricingGroupPriceTableResponseDTO> findPriceHistory(
            @Parameter(description = "Identificador do grupo de preço", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(pricingService.findPriceHistoryByGroup(id));
    }
}
