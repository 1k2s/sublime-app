package br.com.senai.sublime_app.pricing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.pricing.dto.TechniquePriceTableResponseDTO;
import br.com.senai.sublime_app.pricing.dto.TechniqueRequestDTO;
import br.com.senai.sublime_app.pricing.dto.TechniqueResponseDTO;
import br.com.senai.sublime_app.pricing.service.PricingService;
import br.com.senai.sublime_app.pricing.service.TechniqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/techniques")
@Tag(name = "Técnicas e preços", description = """
        Cadastro de técnicas e consulta das tabelas de preço vigentes.
        Fluxo da tela de contrato: listar técnicas → escolher uma → buscar os preços do grupo dela.
        """)
public class TechniqueController {

    private final TechniqueService techniqueService;
    private final PricingService pricingService;

    public TechniqueController(TechniqueService techniqueService, PricingService pricingService) {
        this.techniqueService = techniqueService;
        this.pricingService = pricingService;
    }

    @Operation(summary = "Cadastrar técnica", description = """
            Cria uma técnica ativa vinculada a um grupo de preço. A técnica passa a usar a
            tabela de preço do grupo. O grupo **não pode ser alterado depois**: para
            reclassificar, cadastre uma nova técnica e inative a antiga.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Técnica criada", content = @Content(schema = @Schema(implementation = TechniqueResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Grupo de preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe uma técnica com esse nome", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TechniqueResponseDTO> create(@Valid @RequestBody TechniqueRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(techniqueService.create(dto));
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
        return ResponseEntity.ok(techniqueService.findActive());
    }

    @Operation(summary = "Listar todas as técnicas (cadastro)", description = """
            Retorna **todas** as técnicas, ativas e inativas, ordenadas por nome, com o campo
            `active`. É a listagem da tela de cadastro, onde a clínica encontra uma técnica
            inativa e a reativa (`PATCH /api/techniques/{id}/activate`). Telas de contrato e de
            lançamento usam `GET /api/techniques`, que traz só as ativas.

            Restrita ao perfil administrador quando a autenticação for implementada.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = TechniqueResponseDTO.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<List<TechniqueResponseDTO>> findAll() {
        return ResponseEntity.ok(techniqueService.findAll());
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

    @Operation(summary = "Inativar técnica", description = """
            Realiza o **soft delete** da técnica: ela deixa de aparecer na listagem e não pode
            ser usada em contratos novos, mas contratos e atendimentos antigos continuam
            apontando para ela.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Técnica inativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Técnica não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "A técnica já está inativa", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador da técnica", required = true, example = "1") @PathVariable Long id) {
        techniqueService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reativar técnica", description = """
            Desfaz a inativação: a técnica volta para a listagem e pode entrar em contratos e
            atendimentos novos. O grupo de preço continua o mesmo.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Técnica reativada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Técnica não encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "A técnica já está ativa", content = @Content)
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "Identificador da técnica", required = true, example = "1") @PathVariable Long id) {
        techniqueService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
