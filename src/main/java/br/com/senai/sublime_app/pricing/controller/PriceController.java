package br.com.senai.sublime_app.pricing.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.pricing.dto.DurationPriceRequestDTO;
import br.com.senai.sublime_app.pricing.dto.FrequencyPriceRequestDTO;
import br.com.senai.sublime_app.pricing.dto.SessionPriceResponseDTO;
import br.com.senai.sublime_app.pricing.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/prices")
@Tag(name = "Cadastro de preços", description = """
        Cadastro de valor nas tabelas de preço — fluxo único para preço novo e reajuste.
        Se a combinação já tem linha vigente, ela é fechada (`validTo` = hoje) e a nova
        abre com `validFrom` = hoje. O histórico nunca é alterado.
        """)
public class PriceController {

    private final PricingService pricingService;

    public PriceController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Operation(summary = "Cadastrar valor por duração", description = """
            Cria uma linha em `SessionDurationPrice` para a combinação (grupo, plano, duração).
            O grupo precisa ser `DURATION_BASED` e o plano precisa estar ativo e ter `sessionCount`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Valor cadastrado", content = @Content(schema = @Schema(implementation = SessionPriceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Grupo de preço ou plano não encontrado", content = @Content)
    })
    @PostMapping("/duration")
    public ResponseEntity<SessionPriceResponseDTO> createDurationPrice(@Valid @RequestBody DurationPriceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingService.createDurationPrice(dto));
    }

    @Operation(summary = "Cadastrar valor por frequência", description = """
            Cria uma linha em `SessionFrequencyPrice` para a combinação (grupo, plano, frequência semanal).
            O grupo precisa ser `FREQUENCY_BASED` e o plano precisa estar ativo.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Valor cadastrado", content = @Content(schema = @Schema(implementation = SessionPriceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada", content = @Content),
            @ApiResponse(responseCode = "404", description = "Grupo de preço ou plano não encontrado", content = @Content)
    })
    @PostMapping("/frequency")
    public ResponseEntity<SessionPriceResponseDTO> createFrequencyPrice(@Valid @RequestBody FrequencyPriceRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingService.createFrequencyPrice(dto));
    }
}
