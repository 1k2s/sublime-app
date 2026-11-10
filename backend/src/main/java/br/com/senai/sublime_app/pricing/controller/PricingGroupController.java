package br.com.senai.sublime_app.pricing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.pricing.dto.PricingGroupRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PricingGroupResponseDTO;
import br.com.senai.sublime_app.pricing.service.PricingGroupService;
import io.swagger.v3.oas.annotations.Operation;
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

    public PricingGroupController(PricingGroupService pricingGroupService) {
        this.pricingGroupService = pricingGroupService;
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
}
