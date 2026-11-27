package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractAmendmentPreviewRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractPreviewRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractPreviewResponseDTO;
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
            Cria um contrato com um plano e um item por grupo de preço, e responde com ele. Cada
            item informa a técnica e uma linha de preço (`GET /api/techniques/{id}/prices`): no
            item por duração, `sessionDurationPriceId` + `frequency`; no Pilates em Grupo,
            `sessionFrequencyPriceId`. O `firstDueDate` (1º vencimento) e o `applyCredit` (aplicar o
            crédito de contratos anteriores do titular) são opcionais.

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
            Retorna a versão atual de cada contrato, com o status, incluindo as canceladas. As
            versões anteriores ficam em `GET /api/contracts/{id}/versions`.

            Regras: "Contract e ContractItem" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @Operation(summary = "Buscar contrato por ID", description = """
            Retorna uma versão do contrato, com o cabeçalho e os itens.

            Regras: "Contract e ContractItem" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @Operation(summary = "Versões do contrato (histórico)", description = """
            Retorna a versão informada e as anteriores, da mais nova para a mais antiga. Pedida
            pela versão atual, é o histórico inteiro.

            Regras: "Versionamento (aditivos)" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versões retornadas", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<ContractResponseDTO>> findVersions(
            @Parameter(description = "Identificador da versão (normalmente a atual)", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.findVersions(id));
    }

    @Operation(summary = "Prévia do contrato (tela de criação)", description = """
            Calcula, sem gravar, o que o cadastro calcularia: o fim, as opções do plano do Pilates
            em Grupo, o valor de cada item, o total, as parcelas e o cronograma. Envie o plano, o
            início, os itens já escolhidos (no formato do cadastro) e, opcionalmente, as parcelas,
            o `firstDueDate`, o `patientId` (para trazer o crédito disponível do titular) e o
            `applyCredit`.

            Regras: "Valor e parcelas" em Contract, no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prévia calculada", content = @Content(schema = @Schema(implementation = ContractPreviewResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do contrato violada (o `detail` diz qual)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Plano, técnica ou preço não encontrado", content = @Content)
    })
    @PostMapping("/preview")
    public ResponseEntity<ContractPreviewResponseDTO> preview(@RequestBody @Valid ContractPreviewRequestDTO requestDTO) {
        return ResponseEntity.ok(contractService.preview(requestDTO));
    }

    @Operation(summary = "Alterar contrato (aditivo)", description = """
            Gera uma nova versão do contrato e responde com ela; a versão atual é inativada. Envie
            o estado completo da nova versão (o item que não vier é removido) e, opcionalmente, o
            `endDate` (prorrogação) e o `firstDueDate`.

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

    @Operation(summary = "Prévia do aditivo", description = """
            Calcula, sem gravar, o que o aditivo calcularia: o novo fim, as opções do plano do
            Pilates em Grupo, o valor de cada item, o total, o pago até agora, as parcelas e o
            cronograma (com o 1º vencimento sugerido). Envie os itens do formulário (no formato do
            aditivo) e, opcionalmente, o `endDate` (prorrogação), as parcelas e o `firstDueDate`.
            A versão continua ativa.

            Regras: "Versionamento (aditivos)" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prévia calculada", content = @Content(schema = @Schema(implementation = ContractPreviewResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do aditivo violada (o `detail` diz qual)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Contrato, técnica ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Versão inativa (substituída por aditivo ou inativada)", content = @Content)
    })
    @PostMapping("/{id}/amendments/preview")
    public ResponseEntity<ContractPreviewResponseDTO> previewAmendment(
            @Parameter(description = "Identificador da versão atual do contrato", required = true, example = "1") @PathVariable Long id,
            @RequestBody @Valid ContractAmendmentPreviewRequestDTO requestDTO) {
        return ResponseEntity.ok(contractService.previewAmendment(id, requestDTO));
    }

    @Operation(summary = "Cancelar contrato", description = """
            Cancela o contrato (soft delete): ele continua no histórico, com o status "Cancelado".

            Regras: "Status do contrato" em Contract, no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contrato cancelado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O contrato já está inativo (substituído por aditivo ou já cancelado)", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        contractService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
