package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractPreviewRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractPreviewResponseDTO;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.dto.ContractGroupPlanOptionsResponseDTO;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contratos", description = """
        Contratos entre paciente e clínica: cabeçalho e um item por grupo de preço, versionados
        por aditivos. Regras: "Contract e ContractItem" no `docs/domain-model.md`.
        """)
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "Criar contrato", description = """
            Cria um contrato com um plano e um item por grupo de preço. Cada item informa a
            técnica e uma linha de preço vigente (`GET /api/techniques/{id}/prices`): por
            duração, `sessionDurationPriceId` + `frequency`; Pilates em Grupo,
            `sessionFrequencyPriceId`. O fim, o total e o valor das parcelas são calculados (ver
            `preview`); `installments` vai de 1 até os meses do contrato.

            Regras: "Contract e ContractItem" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do contrato violada (o `detail` diz qual)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente, plano, técnica ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Paciente já possui um contrato vigente, ou o beneficiário já está em outro contrato vigente", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ContractResponseDTO> create(@RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar contratos", description = """
            Retorna todos os contratos cadastrados, incluindo ativos e inativos (versões
            anteriores). `current`: vigente hoje (ativo e com o fim a partir de hoje); ativo com
            `current` falso = vencido. `totalValue` e o valor das parcelas são calculados pelas
            linhas de preço travadas nos itens.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @Operation(summary = "Buscar contrato por ID", description = "Retorna o cabeçalho e os itens de um contrato, com o `current` (vigente hoje; ver \"Listar contratos\").")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @Operation(summary = "Prévia do contrato (tela de criação)", description = """
            Calcula, sem gravar e com as mesmas contas do cadastro: vigência, opções do plano do
            Pilates em Grupo, valor de cada item, total e parcelas. Envie o plano, o início e os
            itens já escolhidos (no formato do cadastro; a lista pode vir incompleta) e,
            opcionalmente, as parcelas (vazio ou acima do máximo = o máximo do contrato; a
            resposta traz as aplicadas).

            Regras: "Valor e parcelas" em Contract, no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prévia calculada", content = @Content(schema = @Schema(implementation = ContractPreviewResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, plano avulso, frequência acima do prazo máximo ou pacote sem item por duração", content = @Content),
            @ApiResponse(responseCode = "404", description = "Plano, técnica ou preço não encontrado", content = @Content)
    })
    @PostMapping("/preview")
    public ResponseEntity<ContractPreviewResponseDTO> preview(@RequestBody @Valid ContractPreviewRequestDTO requestDTO) {
        return ResponseEntity.ok(contractService.preview(requestDTO));
    }

    @Operation(summary = "Opções do plano do Grupo (aditivo)", description = """
            Opções do plano do Pilates em Grupo para um aditivo, pela vigência atual (sem
            gravar).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções calculadas", content = @Content(schema = @Schema(implementation = ContractGroupPlanOptionsResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}/group-plan-options")
    public ResponseEntity<ContractGroupPlanOptionsResponseDTO> groupPlanOptions(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.groupPlanOptionsFor(id));
    }

    @Operation(summary = "Alterar contrato (aditivo)", description = """
            Gera uma nova versão do contrato (a atual é inativada) e responde com ela. Envie o
            **estado completo**: beneficiário, forma de pagamento e todos os itens (item que não
            vier é removido). Titular, plano e início são copiados. `endDate` (opcional) só
            estende o fim.

            Regras: "Versionamento (aditivos)" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nova versão criada; a versão anterior foi inativada", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do aditivo violada (o `detail` diz qual)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Contrato, técnica, beneficiário ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Versão inativa, aditivo simultâneo ou beneficiário em outro contrato vigente", content = @Content)
    })
    @PostMapping("/{id}/amendments")
    public ResponseEntity<ContractResponseDTO> amend(
            @Parameter(description = "Identificador da versão atual do contrato", required = true, example = "1") @PathVariable Long id,
            @RequestBody @Valid ContractAmendmentRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.amend(id, requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Inativar contrato", description = """
            Soft delete: o contrato fica no histórico.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrato inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O contrato já está inativo (substituído por aditivo ou já encerrado)", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        contractService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
