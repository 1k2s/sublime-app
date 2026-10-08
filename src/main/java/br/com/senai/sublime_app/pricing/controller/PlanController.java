package br.com.senai.sublime_app.pricing.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.pricing.dto.PlanRequestDTO;
import br.com.senai.sublime_app.pricing.dto.PlanResponseDTO;
import br.com.senai.sublime_app.pricing.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "Planos", description = "Cadastro e consulta dos planos usados nas tabelas de preço.")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @Operation(summary = "Cadastrar plano", description = """
            Cria um plano ativo. `sessionCount` é a quantidade de sessões do pacote:
            obrigatório para usar o plano em preço por duração (`DURATION_BASED`) e
            deve ficar nulo nos planos do Pilates em Grupo (mensalidade, `FREQUENCY_BASED`).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano criado", content = @Content(schema = @Schema(implementation = PlanResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe um plano com esse nome", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PlanResponseDTO> create(@Valid @RequestBody PlanRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(dto));
    }

    @Operation(summary = "Listar planos", description = "Retorna todos os planos, ativos e inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = PlanResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<PlanResponseDTO>> findAll() {
        return ResponseEntity.ok(planService.findAll());
    }
}
