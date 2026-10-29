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
        Cadastro e consulta dos grupos de precificação. Técnicas do mesmo grupo
        compartilham a mesma tabela de preço.
        """)
public class PricingGroupController {

    private final PricingGroupService pricingGroupService;

    public PricingGroupController(PricingGroupService pricingGroupService) {
        this.pricingGroupService = pricingGroupService;
    }

    @Operation(summary = "Cadastrar grupo de preço", description = """
            Cria um grupo de precificação. O `pricingModel` define a tabela de preço do grupo
            e **não pode ser alterado depois**:
            - `DURATION_BASED` → preços por duração da sessão × plano
            - `FREQUENCY_BASED` → preços por frequência semanal × plano
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
