package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractAmendmentRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.dto.ContractTermPreviewResponseDTO;
import br.com.senai.sublime_app.contract.enums.SessionFrequency;
import br.com.senai.sublime_app.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
@Tag(name = "Contratos", description = """
        Contratos entre paciente e clínica. Um contrato tem um **cabeçalho** (titular,
        beneficiário, plano único, início, fim calculado, forma de pagamento) e **itens** —
        um por grupo de preço contratado, cada um travando a sua linha de preço.
        Contratos são versionados: alterações geram uma nova versão (aditivo), nunca
        editam o registro existente.
        """)
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "Criar contrato", description = """
            Cria um contrato com um plano único e um ou mais itens (um por grupo de preço).

            **Itens:** cada item informa a técnica (só informativa — aparece no contrato
            impresso) e **exatamente uma** linha de preço vigente (obtida em
            `GET /api/techniques/{id}/prices`):
            - `sessionDurationPriceId` + `frequency` → grupos por duração. A linha precisa ser
              **do plano do contrato**; a frequência define o prazo do item.
            - `sessionFrequencyPriceId` (sem `frequency`) → Pilates em Grupo. O plano do item
              vem das opções de `GET /api/contracts/term-preview`.

            **Fim calculado:** não é enviado. É o início + o maior prazo entre os itens por
            duração (sessões do plano ÷ ritmo, em semanas, até 52) − 1 dia. Contrato só com
            Pilates em Grupo usa um plano de mensalidade e o prazo dele.

            **Restrições:**
            - O titular não pode ter outro contrato **vigente** (ativo e com fim >= hoje).
            - Titular, beneficiário, plano e técnicas devem estar ativos; o plano nunca é o avulso.
            - Um item por grupo de preço; contrato de pacote precisa de pelo menos um item por duração.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrato criado com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida — formato dos itens, preço não vigente ou de outro plano, técnica fora do grupo, frequência acima do prazo máximo, plano do Grupo fora das opções, cadastros inativos ou plano avulso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente, plano, técnica ou preço não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Paciente já possui um contrato vigente", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ContractResponseDTO> create(@RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar contratos", description = "Retorna todos os contratos cadastrados, incluindo ativos e inativos (versões anteriores).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @Operation(summary = "Buscar contrato por ID", description = "Retorna o cabeçalho e os itens de um contrato.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrato encontrado", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    @Operation(summary = "Prévia da vigência (tela de criação)", description = """
            Calcula, sem gravar nada, a vigência do contrato e as opções do plano do item do
            Pilates em Grupo — a mesma conta usada na criação.

            Informe o plano, o início e a frequência de **cada item por duração** (ex:
            `frequencies=ONCE_A_WEEK,BIWEEKLY`). Plano de mensalidade (contrato só com Grupo)
            não precisa de frequências.

            Opções do Grupo: mensalidades até a primeira que cubra a vigência; a sugestão é o
            equivalente do plano do contrato (de-para).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prévia calculada", content = @Content(schema = @Schema(implementation = ContractTermPreviewResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Plano avulso, frequência acima do prazo máximo ou plano de pacote sem frequências", content = @Content),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado", content = @Content)
    })
    @GetMapping("/term-preview")
    public ResponseEntity<ContractTermPreviewResponseDTO> previewTerm(
            @Parameter(description = "Plano do contrato", required = true, example = "2") @RequestParam Long planId,
            @Parameter(description = "Data de início (AAAA-MM-DD)", required = true, example = "2026-11-03")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Frequência de cada item por duração") @RequestParam(required = false) List<SessionFrequency> frequencies) {
        return ResponseEntity.ok(contractService.previewTerm(planId, startDate, frequencies));
    }

    @Operation(summary = "Opções do plano do Grupo (aditivo)", description = """
            Opções do plano do item do Pilates em Grupo para um aditivo, pela vigência atual
            do contrato (nada é gravado).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opções calculadas", content = @Content(schema = @Schema(implementation = ContractTermPreviewResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/{id}/group-plan-options")
    public ResponseEntity<ContractTermPreviewResponseDTO> groupPlanOptions(
            @Parameter(description = "Identificador do contrato", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.groupPlanOptionsFor(id));
    }

    @Operation(summary = "Alterar contrato (aditivo)", description = """
            O contrato **nunca é editado no lugar**. Cada alteração gera uma **nova versão**
            (novo registro, com `previousContractId` apontando para a versão atual), e a
            versão atual é inativada. Responde com a nova versão.

            **Estado completo:** envie o beneficiário, a forma de pagamento e **todos** os itens
            da nova versão (o frontend abre o formulário preenchido com a versão atual). Item
            que não vier é removido; item de grupo novo é incluído.

            **Nunca mudam** (são copiados): titular, plano e início. Trocar o plano = encerrar o
            contrato e fazer outro.

            **O que é igual à versão atual é aceito como está:** manter a mesma linha de preço
            é permitido mesmo após um reajuste; trocar de linha exige uma linha vigente. O mesmo
            vale para técnica e beneficiário inativos.

            **Fim:** mantido por padrão. `endDate` (opcional) só estende — prorrogação, depois do
            fim atual. Incluir um item por duração pode estender o fim (prazo do item contado a
            partir de hoje). Remover item ou mudar frequência não muda o fim.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nova versão criada; a versão anterior foi inativada", content = @Content(schema = @Schema(implementation = ContractResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida — mesmas regras da criação, fim pedido não posterior ao atual, ou item por duração em contrato de mensalidade", content = @Content),
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
