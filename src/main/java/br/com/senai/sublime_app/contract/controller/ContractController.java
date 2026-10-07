package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
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
        Contratos são versionados: alterações geram uma nova versão (aditivo),
        nunca editam o registro existente.
        """)
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "Criar contrato", description = """
            Cria um contrato vinculando o paciente titular a uma técnica âncora e a uma linha de preço vigente
            (obtida em `GET /api/techniques/{id}/prices`).

            **Regra de Exclusive Arc:** exatamente um dos dois campos abaixo deve ser preenchido:
            - `sessionDurationPriceId` → grupos de precificação por **duração** (DURATION_BASED)
            - `sessionFrequencyPriceId` → grupos de precificação por **frequência** (FREQUENCY_BASED)

            **A linha de preço é a fonte da verdade:** o plano do contrato é copiado dela.
            - Preço por **duração**: `weeklyFrequency` é **obrigatório** (negociação, não afeta o preço).
            - Preço por **frequência**: `weeklyFrequency` **não deve ser enviado** — é copiado da linha de preço.

            **Snapshot de preço:** o preço é travado no momento da assinatura.
            Reajustes futuros no catálogo não afetam este contrato.

            **Restrições:**
            - O paciente titular não pode ter outro contrato **vigente** (ativo e com `endDate` >= hoje).
              Um contrato ativo porém vencido não bloqueia um novo.
            - Paciente titular, beneficiário e técnica devem estar ativos.
            - `endDate` não pode ser anterior a `startDate`.
            - O preço informado deve estar vigente (`validTo` nulo).
            - A técnica âncora deve pertencer ao grupo de precificação da linha de preço.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida — violação do exclusive arc, preço não vigente, técnica fora do grupo do preço, weeklyFrequency ausente/indevido, paciente/beneficiário/técnica inativos, datas invertidas ou campos obrigatórios ausentes", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente, técnica ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Paciente já possui um contrato vigente", content = @Content)
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

    @Operation(summary = "Alterar contrato (aditivo)", description = """
            O contrato **nunca é editado no lugar**. Cada alteração gera uma **nova versão**
            (novo registro, com `previousContractId` apontando para a versão atual), e a
            versão atual é inativada. Responde com a nova versão.

            **Estado completo:** envie todos os campos da nova versão (o frontend abre o
            formulário preenchido com a versão atual). O titular não é enviado: é copiado
            da versão atual — trocar o titular exige um contrato novo.

            **Regras:** as mesmas da criação (exclusive arc, `weeklyFrequency`, técnica do
            grupo do preço, datas), com uma diferença: o que for **igual à versão atual é
            aceito como está**.
            - Manter a mesma linha de preço é permitido mesmo após um reajuste; trocar de
              linha exige uma linha vigente.
            - Manter a mesma técnica/beneficiário é permitido mesmo se inativos; trocar
              exige que estejam ativos.

            **Prorrogação:** um contrato ativo porém vencido pode ser alterado (ex: novo `endDate`).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nova versão criada; a versão anterior foi inativada", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida — mesmas regras da criação", content = @Content),
            @ApiResponse(responseCode = "404", description = "Contrato, técnica, beneficiário ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Contrato inativo (já substituído ou encerrado) ou alterado simultaneamente", content = @Content)
    })
    @PostMapping("/{id}/amendments")
    public ResponseEntity<ContractResponseDTO> amend(
            @Parameter(description = "Identificador da versão atual do contrato", required = true, example = "1") @PathVariable Long id,
            @RequestBody @Valid ContractAmendmentRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.amend(id, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        contractService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}