package br.com.senai.sublime_app.consultation.controller;

import br.com.senai.sublime_app.consultation.dto.ConsultationRequestDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ContractSessionBalanceResponseDTO;
import br.com.senai.sublime_app.consultation.dto.EligibleContractResponseDTO;
import br.com.senai.sublime_app.consultation.service.ConsultationService;
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
@RequestMapping("/api/consultations")
@Tag(name = "Atendimentos", description = """
        Lançamento dos atendimentos cobrados. O sistema descobre de qual contrato a sessão
        sai, resolve o preço, debita o saldo de sessões do item e grava o repasse devido ao
        prestador (sem aprovação). O lançamento nunca é editado: para corrigir, inative e
        lance de novo.
        """)
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @Operation(summary = "Lançar atendimento", description = """
            Lança um atendimento cobrado: `ATTENDED` (aconteceu), `NO_SHOW` (faltou sem aviso)
            ou `CANCELED_LATE` (desmarcou com menos de 6h). Os três são cobrados e geram repasse.

            **Contrato:** consulte antes `GET /api/consultations/eligible-contracts`. Se o
            paciente tiver contrato vinculado na data (como titular ou beneficiário),
            `contractId` é **obrigatório** e precisa ser um deles; sem contrato vinculado, envie
            vazio (Avulso).

            **Preço** (resolvido pelo sistema, nunca enviado):
            - o contrato cobre o grupo da técnica → preço do item (travado na mesma duração; atual
              em outra duração) e **debita uma sessão**;
            - o contrato não cobre o grupo → de-para do plano do contrato, preço atual (Pilates em
              Grupo: 1x por semana); não debita;
            - sem contrato → preço atual do Avulso; o Pilates em Grupo exige contrato.

            **Comissão:** pela origem do titular do contrato (sem contrato, do paciente atendido).
            Repasse = valor × percentual, arredondado para 2 casas (`HALF_UP`).

            **Resposta:** `remainingSessions` traz o saldo do item depois do lançamento, quando ele
            debitou um item por duração. Negativo = saldo esgotado (o lançamento é aceito).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atendimento lançado", content = @Content(schema = @Schema(implementation = ConsultationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida — data futura, contrato ausente ou não vinculado ao paciente na data, sem preço para a combinação, plano sem equivalente, Pilates em Grupo sem contrato, ou paciente, prestador ou técnica inativos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente, prestador ou técnica não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ConsultationResponseDTO> create(@RequestBody @Valid ConsultationRequestDTO requestDTO) {
        ConsultationResponseDTO response = consultationService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar atendimentos", description = """
            Lista os atendimentos, ativos e inativos, do mais recente para o mais antigo. Todos
            os filtros são opcionais e se combinam.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ConsultationResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<ConsultationResponseDTO>> search(
            @Parameter(description = "Prestador que atendeu", example = "1") @RequestParam(required = false) Long providerId,
            @Parameter(description = "Paciente atendido", example = "1") @RequestParam(required = false) Long patientId,
            @Parameter(description = "Data inicial do atendimento, inclusiva (AAAA-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final do atendimento, inclusiva (AAAA-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(consultationService.search(providerId, patientId, from, to));
    }

    @Operation(summary = "Contratos vinculados (modal do lançamento)", description = """
            Lista, sem gravar nada, os contratos vinculados ao paciente na data do atendimento:
            versões ativas que cobrem a data, com ele como titular ou beneficiário (no máximo
            dois). Para cada um: se ele cobre o grupo da técnica (`CONTRACT_ITEM`, debita o item)
            ou vai pelo de-para (`EQUIVALENT_PLAN`), e o saldo do item, quando houver.

            Lista vazia → Avulso (sem `contractId`); um → selecionado automaticamente; dois → o
            usuário escolhe livremente.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contratos vinculados", content = @Content(schema = @Schema(implementation = EligibleContractResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Paciente ou técnica não encontrado", content = @Content)
    })
    @GetMapping("/eligible-contracts")
    public ResponseEntity<List<EligibleContractResponseDTO>> findEligibleContracts(
            @Parameter(description = "Paciente que compareceu", required = true, example = "1") @RequestParam Long patientId,
            @Parameter(description = "Técnica atendida", required = true, example = "1") @RequestParam Long techniqueId,
            @Parameter(description = "Data do atendimento (AAAA-MM-DD)", required = true, example = "2026-10-21")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurredAt) {
        return ResponseEntity.ok(consultationService.findEligibleContracts(patientId, techniqueId, occurredAt));
    }

    @Operation(summary = "Saldo de sessões do contrato", description = """
            Saldo de cada item por duração do contrato (sessões do plano − sessões usadas),
            somando os atendimentos de toda a cadeia de versões até a versão informada (use a
            versão ativa). O saldo pode ser negativo. O item do Pilates em Grupo não tem saldo e
            não aparece.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo calculado", content = @Content(schema = @Schema(implementation = ContractSessionBalanceResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/session-balance")
    public ResponseEntity<List<ContractSessionBalanceResponseDTO>> findSessionBalance(
            @Parameter(description = "Identificador do contrato (versão ativa)", required = true, example = "1") @RequestParam Long contractId) {
        return ResponseEntity.ok(consultationService.findSessionBalance(contractId));
    }

    @Operation(summary = "Buscar atendimento por ID", description = "Retorna um atendimento lançado, ativo ou inativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento encontrado", content = @Content(schema = @Schema(implementation = ConsultationResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponseDTO> findById(
            @Parameter(description = "Identificador do atendimento", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(consultationService.findById(id));
    }

    @Operation(summary = "Inativar atendimento", description = """
            Realiza o **soft delete** do atendimento: o registro é marcado como `active = false`
            e deixa de contar no saldo de sessões e no repasse. O lançamento nunca é editado:
            para corrigir, inative e lance de novo (o erro e a correção ficam registrados).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Atendimento inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O atendimento já está inativo", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do atendimento", required = true, example = "1") @PathVariable Long id) {
        consultationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
