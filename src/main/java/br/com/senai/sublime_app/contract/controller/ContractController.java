package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "Cria um novo contrato", description = """
            Cria um contrato vinculando o paciente titular a um plano, técnica âncora e preço vigente.

            **Regra de Exclusive Arc:** exatamente um dos dois campos abaixo deve ser preenchido — nunca os dois, nunca nenhum:
            - `groupPlanPriceId` → para grupos de precificação por **duração** (DURATION_BASED)
            - `groupPlanFrequencyPriceId` → para grupos de precificação por **frequência** (FREQUENCY_BASED)

            **Snapshot de preço:** o preço é travado no momento da assinatura. Reajustes futuros no catálogo não afetam este contrato.

            **Restrições:**
            - O paciente titular não pode ter outro contrato ativo.
            - O preço informado deve estar vigente (sem data de encerramento).
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

    /**
     * Lista todos os contratos.
     *
     * @return lista de contratos com status 200
     */
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    /**
     * Busca um contrato pelo ID.
     *
     * @param id identificador do contrato
     * @return contrato encontrado com status 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    /**
     * Atualiza os dados de negociação de um contrato (frequência, datas,
     * pagamento).
     * Os campos de preço são imutáveis após a assinatura.
     *
     * @param id         identificador do contrato
     * @param requestDTO novos dados de negociação
     * @return contrato atualizado com status 200
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> update(@PathVariable Long id,
            @RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.update(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Inativa um contrato (soft delete).
     *
     * @param id identificador do contrato
     * @return status 204 sem corpo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }
}