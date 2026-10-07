package br.com.senai.sublime_app.provider.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.provider.dto.ProviderRequestDTO;
import br.com.senai.sublime_app.provider.dto.ProviderResponseDTO;
import br.com.senai.sublime_app.provider.dto.ProviderUpdateRequestDTO;
import br.com.senai.sublime_app.provider.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Prestadores", description = """
        Cadastro dos prestadores que realizam os atendimentos. Cada prestador é vinculado a
        exatamente um usuário (que pode ter role `PROVIDER` ou `ADMIN`), e o vínculo é fixo.
        O percentual de comissão é uniforme (não varia por técnica) e entra no repasse de
        cada atendimento.
        """)
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Operation(summary = "Cadastrar prestador", description = """
            Cria um prestador ativo vinculado a um usuário existente e **ativo**. Um usuário
            só pode ter um prestador. Para remover o acesso de um prestador, inative o
            usuário dele (`DELETE /api/users/{id}`).

            `commissionPercentage` no formato 0–100 (ex: `35.00` = 35%), com no máximo 2 casas
            decimais — valores com mais casas são rejeitados, nunca arredondados.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prestador criado", content = @Content(schema = @Schema(implementation = ProviderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos (incl. comissão fora de 0–100 ou com mais de 2 casas) ou usuário inativo", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O usuário já está vinculado a um prestador", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ProviderResponseDTO> create(@Valid @RequestBody ProviderRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.create(dto));
    }

    @Operation(summary = "Listar prestadores", description = "Retorna todos os prestadores cadastrados, incluindo ativos e inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ProviderResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<ProviderResponseDTO>> findAll() {
        return ResponseEntity.ok(providerService.findAll());
    }

    @Operation(summary = "Buscar prestador por ID", description = "Retorna o prestador mesmo que esteja inativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prestador encontrado", content = @Content(schema = @Schema(implementation = ProviderResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Prestador não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponseDTO> findById(
            @Parameter(description = "Identificador do prestador", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(providerService.findById(id));
    }

    @Operation(summary = "Atualizar prestador", description = """
            Atualiza o nome e o percentual de comissão. O usuário vinculado **não pode ser
            alterado** e por isso não é enviado.

            A nova comissão vale para os próximos atendimentos: os já lançados guardam a
            comissão aplicada no momento do lançamento.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prestador atualizado", content = @Content(schema = @Schema(implementation = ProviderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Prestador não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponseDTO> update(
            @Parameter(description = "Identificador do prestador", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody ProviderUpdateRequestDTO dto) {
        return ResponseEntity.ok(providerService.update(id, dto));
    }

    @Operation(summary = "Inativar prestador", description = """
            Realiza o **soft delete** do prestador: ele é marcado como inativo, mas os
            atendimentos já lançados continuam apontando para ele.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Prestador inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Prestador não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do prestador", required = true, example = "1") @PathVariable Long id) {
        providerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
