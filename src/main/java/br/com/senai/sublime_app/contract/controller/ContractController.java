package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contratos", description = """
        Gerenciamento de contratos entre paciente e clínica.
        Cada contrato trava o preço vigente no momento da assinatura e aplica
        a regra de **exclusive arc** nas referências de preço.
        """)
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "Criar contrato", description = """
            Cria um contrato vinculando o paciente titular a um plano, técnica âncora e preço vigente.

            **Regra de Exclusive Arc:** exatamente um dos dois campos abaixo deve ser preenchido:
            - `groupPlanPriceId` → grupos de precificação por **duração** (DURATION_BASED)
            - `groupPlanFrequencyPriceId` → grupos de precificação por **frequência** (FREQUENCY_BASED)

            **Snapshot de preço:** o preço é travado no momento da assinatura.
            Reajustes futuros no catálogo não afetam este contrato.

            **Restrições:**
            - O paciente titular não pode ter outro contrato ativo.
            - O preço informado deve estar vigente (`validTo` nulo).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida — violação do exclusive arc, preço não vigente ou campos obrigatórios ausentes", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente, técnica, plano ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Paciente já possui um contrato ativo", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ContractResponseDTO> create(@RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar contratos", description = "Retorna todos os contratos cadastrados, incluindo ativos e inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @Operation(summary = "Buscar contrato por ID", description = "Retorna os dados completos de um contrato específico pelo seu identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @Operation(summary = "Atualizar contrato", description = """
            Atualiza os dados de negociação de um contrato existente.

            **Campos permitidos para atualização:**
            - `weeklyFrequency` — frequência semanal de sessões
            - `startDate` / `endDate` — vigência negociada
            - `paymentMethod` — método de pagamento

            **Campos imutáveis após a assinatura:**
            Os campos `groupPlanPriceId` e `groupPlanFrequencyPriceId` são ignorados
            nesta operação — o snapshot de preço não pode ser alterado retroativamente.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato atualizado com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição", content = @Content),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> update(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id,
            @RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.update(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Inativar contrato", description = """
            Realiza o **soft delete** do contrato: o registro é marcado como `active = false`
            e preservado no histórico para fins de rastreabilidade e auditoria.
            O contrato não é removido fisicamente do banco de dados.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrato inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }
}