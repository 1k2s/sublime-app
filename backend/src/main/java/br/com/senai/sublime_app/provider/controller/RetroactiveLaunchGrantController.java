package br.com.senai.sublime_app.provider.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.provider.dto.RetroactiveGrantRequestDTO;
import br.com.senai.sublime_app.provider.dto.RetroactiveGrantResponseDTO;
import br.com.senai.sublime_app.provider.service.RetroactiveLaunchGrantService;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/providers/{id}/retroactive-grant")
@Tag(name = "Prestadores")
public class RetroactiveLaunchGrantController {

    private final RetroactiveLaunchGrantService grantService;

    public RetroactiveLaunchGrantController(RetroactiveLaunchGrantService grantService) {
        this.grantService = grantService;
    }

    @Operation(summary = "Liberar lançamentos retroativos", description = """
            O prestador só lança atendimentos com a data de hoje. A liberação permite
            `allowedCount` lançamentos (1 a 50) em qualquer data passada, até 23:59 de hoje.
            Liberar de novo substitui a anterior. Desfazer um lançamento não devolve a unidade.

            **Perfis:** ADMIN.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Liberação criada", content = @Content(schema = @Schema(implementation = RetroactiveGrantResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Quantidade inválida ou prestador inativo", content = @Content),
            @ApiResponse(responseCode = "404", description = "Prestador não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<RetroactiveGrantResponseDTO> grant(
            @Parameter(description = "Identificador do prestador", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody RetroactiveGrantRequestDTO dto, CurrentUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(grantService.grant(id, dto.allowedCount(), currentUser));
    }

    @Operation(summary = "Liberação ativa", description = """
            A liberação de hoje com lançamentos restantes; 204 quando não há.

            **Perfis:** ADMIN.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liberação ativa", content = @Content(schema = @Schema(implementation = RetroactiveGrantResponseDTO.class))),
            @ApiResponse(responseCode = "204", description = "Sem liberação ativa"),
            @ApiResponse(responseCode = "404", description = "Prestador não encontrado", content = @Content)
    })
    @GetMapping
    public ResponseEntity<RetroactiveGrantResponseDTO> findActive(
            @Parameter(description = "Identificador do prestador", required = true, example = "1") @PathVariable Long id) {
        return grantService.findActive(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
