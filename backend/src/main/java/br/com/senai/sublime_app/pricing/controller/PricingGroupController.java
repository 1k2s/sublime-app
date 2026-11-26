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
            Cadastra o grupo e responde com ele. O `pricingModel` (`DURATION_BASED` ou
            `FREQUENCY_BASED`) define a tabela de preço do grupo.

            Regras: "PricingGroup" no `docs/domain-model.md`.
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
            Retorna as linhas de preço vigentes do grupo. Para o preço de um contrato a partir da
            técnica, use `GET /api/techniques/{id}/prices`.

            Regras: "PricingGroup" no `docs/domain-model.md`.
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
            Retorna todas as linhas de preço do grupo, da mais recente para a mais antiga, cada
            uma com o `status` (`CURRENT`, `EXPIRED` ou `CLOSED`). Usado pela tela de preços.

            Regras: "Status da linha de preço" no `docs/domain-model.md`.
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
