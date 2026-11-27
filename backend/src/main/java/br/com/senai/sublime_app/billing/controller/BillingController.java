package br.com.senai.sublime_app.billing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.billing.dto.AccountListItemResponseDTO;
import br.com.senai.sublime_app.billing.dto.BalanceTransferResponseDTO;
import br.com.senai.sublime_app.billing.dto.ContractSessionsResponseDTO;
import br.com.senai.sublime_app.billing.dto.ContractVersionInstallmentsResponseDTO;
import br.com.senai.sublime_app.billing.dto.PatientAccountResponseDTO;
import br.com.senai.sublime_app.billing.service.BillingAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Financeiro", description = """
        A conta dos pacientes, calculada na leitura. Regras: "Payment" no `docs/domain-model.md`.
        """)
public class BillingController {

    private final BillingAccountService billingAccountService;

    public BillingController(BillingAccountService billingAccountService) {
        this.billingAccountService = billingAccountService;
    }

    @Operation(summary = "Listar contas", description = """
            Retorna uma linha por paciente com conta, com o saldo, a situação, o próximo vencimento
            e os avisos, já na ordem da lista.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = AccountListItemResponseDTO.class)))
    })
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountListItemResponseDTO>> findAccounts() {
        return ResponseEntity.ok(billingAccountService.findAccounts());
    }

    @Operation(summary = "Conta do paciente", description = """
            Retorna a conta de cada contrato do paciente e a do avulso (nula, se ele não tiver).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conta retornada com sucesso", content = @Content(schema = @Schema(implementation = PatientAccountResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content)
    })
    @GetMapping("/patients/{id}/account")
    public ResponseEntity<PatientAccountResponseDTO> findPatientAccount(
            @Parameter(description = "Identificador do paciente", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(billingAccountService.findPatientAccount(id));
    }

    @Operation(summary = "Transferências de saldo do paciente", description = """
            Retorna o crédito de contratos anteriores aplicado nos contratos do titular, da mais
            recente para a mais antiga.

            Regras: "Payment" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = BalanceTransferResponseDTO.class)))
    })
    @GetMapping("/patients/{id}/transfers")
    public ResponseEntity<List<BalanceTransferResponseDTO>> findTransfers(
            @Parameter(description = "Identificador do paciente titular", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(billingAccountService.findTransfers(id));
    }

    @Operation(summary = "Parcelas do contrato", description = """
            Retorna as parcelas de cada versão do contrato, da mais nova para a mais antiga, com a
            situação de cada parcela.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Parcelas retornadas com sucesso", content = @Content(schema = @Schema(implementation = ContractVersionInstallmentsResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/contracts/{id}/installments")
    public ResponseEntity<List<ContractVersionInstallmentsResponseDTO>> findInstallments(
            @Parameter(description = "Identificador do contrato (qualquer versão)", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(billingAccountService.findInstallments(id));
    }

    @Operation(summary = "Sessões do contrato", description = """
            Retorna os atendimentos ativos do contrato, numerados por item.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessões retornadas com sucesso", content = @Content(schema = @Schema(implementation = ContractSessionsResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Contrato não encontrado", content = @Content)
    })
    @GetMapping("/contracts/{id}/sessions")
    public ResponseEntity<ContractSessionsResponseDTO> findSessions(
            @Parameter(description = "Identificador do contrato (qualquer versão)", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(billingAccountService.findSessions(id));
    }
}
