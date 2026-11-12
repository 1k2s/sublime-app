package br.com.senai.sublime_app.consultation.controller;

import br.com.senai.sublime_app.consultation.dto.ConsultationPatientResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationRequestDTO;
import br.com.senai.sublime_app.consultation.dto.ConsultationResponseDTO;
import br.com.senai.sublime_app.consultation.dto.ContractSessionBalanceResponseDTO;
import br.com.senai.sublime_app.consultation.dto.EligibleContractResponseDTO;
import br.com.senai.sublime_app.consultation.service.ConsultationService;
import br.com.senai.sublime_app.user.domain.CurrentUser;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@Tag(name = "Atendimentos", description = """
        Lançamento dos atendimentos cobrados (preço, débito de sessão e repasse resolvidos
        pelo sistema). Regras: "Consultation" no `docs/domain-model.md`.
        """)
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @Operation(summary = "Lançar atendimento", description = """
            Lança um atendimento cobrado (`ATTENDED`, `NO_SHOW` ou `CANCELED_LATE`). Preço,
            comissão e débito de sessão são resolvidos pelo sistema; o `contractId` vem do
            `GET /api/consultations/eligible-contracts` (vazio = Avulso).

            **Perfis:** ADMIN (informa o `providerId`) e PROVIDER (só em nome próprio).
            Regras: "Consultation" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atendimento lançado", content = @Content(schema = @Schema(implementation = ConsultationResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do lançamento violada (o `detail` diz qual)", content = @Content),
            @ApiResponse(responseCode = "403", description = "PROVIDER lançando para outro prestador, ou usuário sem prestador vinculado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente, prestador ou técnica não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ConsultationResponseDTO> create(@RequestBody @Valid ConsultationRequestDTO requestDTO,
            CurrentUser currentUser) {
        ConsultationResponseDTO response = consultationService.create(requestDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Listar atendimentos", description = """
            Ativos e inativos, do mais recente para o mais antigo; filtros opcionais.

            **Perfis:** ADMIN e PROVIDER (só os dele).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ConsultationResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Usuário PROVIDER sem prestador vinculado", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<ConsultationResponseDTO>> search(
            @Parameter(description = "Prestador que atendeu", example = "1") @RequestParam(required = false) Long providerId,
            @Parameter(description = "Paciente atendido", example = "1") @RequestParam(required = false) Long patientId,
            @Parameter(description = "Data inicial do atendimento, inclusiva (AAAA-MM-DD)", example = "2026-10-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Data final do atendimento, inclusiva (AAAA-MM-DD)", example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            CurrentUser currentUser) {
        return ResponseEntity.ok(consultationService.search(providerId, patientId, from, to, currentUser));
    }

    @Operation(summary = "Pacientes do lançamento", description = """
            Pacientes ativos, por nome, só com `id`, `name` e `birthDate`.

            **Perfis:** ADMIN e PROVIDER.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = ConsultationPatientResponseDTO.class)))
    })
    @GetMapping("/patients")
    public ResponseEntity<List<ConsultationPatientResponseDTO>> findPatients() {
        return ResponseEntity.ok(consultationService.findPatients());
    }

    @Operation(summary = "Contratos vinculados (modal do lançamento)", description = """
            Contratos do paciente na data (no máximo dois), com a origem do preço e o saldo.
            Vazio = Avulso; um = selecionado; dois = o usuário escolhe.

            **Perfis:** ADMIN e PROVIDER.
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
            Saldo de cada item por duração (pode ser negativo), contando toda a cadeia de
            versões. Informe a versão ativa.

            **Perfis:** ADMIN e PROVIDER.
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

    @Operation(summary = "Buscar atendimento por ID", description = """
            Ativo ou inativo.

            **Perfis:** ADMIN e PROVIDER (o de outro prestador responde 404).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atendimento encontrado", content = @Content(schema = @Schema(implementation = ConsultationResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Usuário PROVIDER sem prestador vinculado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponseDTO> findById(
            @Parameter(description = "Identificador do atendimento", required = true, example = "1") @PathVariable Long id,
            CurrentUser currentUser) {
        return ResponseEntity.ok(consultationService.findById(id, currentUser));
    }

    @Operation(summary = "Inativar atendimento", description = """
            Soft delete: deixa de contar no saldo e no repasse. Para corrigir um lançamento,
            inative e lance de novo.

            **Perfis:** ADMIN e PROVIDER (o de outro prestador responde 404).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Atendimento inativado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Usuário PROVIDER sem prestador vinculado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Atendimento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O atendimento já está inativo", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do atendimento", required = true, example = "1") @PathVariable Long id,
            CurrentUser currentUser) {
        consultationService.deactivate(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
