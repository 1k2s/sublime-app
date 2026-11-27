package br.com.senai.sublime_app.billing.controller;

import java.util.List;

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

import br.com.senai.sublime_app.billing.dto.PaymentPreviewRequestDTO;
import br.com.senai.sublime_app.billing.dto.PaymentPreviewResponseDTO;
import br.com.senai.sublime_app.billing.dto.PaymentRequestDTO;
import br.com.senai.sublime_app.billing.dto.PaymentResponseDTO;
import br.com.senai.sublime_app.billing.service.PaymentService;
import br.com.senai.sublime_app.user.domain.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Pagamentos", description = """
        Pagamentos dos pacientes, de valor livre. Regras: "Payment" no `docs/domain-model.md`.
        """)
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Registrar pagamento", description = """
            Registra um pagamento do paciente e responde com ele. Sem `contractId`, é um pagamento
            do avulso.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento registrado", content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do pagamento violada (o `detail` diz qual)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente ou contrato não encontrado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> register(@RequestBody @Valid PaymentRequestDTO requestDTO,
            CurrentUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.register(requestDTO, currentUser));
    }

    @Operation(summary = "Prévia do pagamento", description = """
            Calcula, sem gravar, a taxa e a previsão de recebimento de um pagamento.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prévia calculada", content = @Content(schema = @Schema(implementation = PaymentPreviewResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra do pagamento violada (o `detail` diz qual)", content = @Content)
    })
    @PostMapping("/preview")
    public ResponseEntity<PaymentPreviewResponseDTO> preview(@RequestBody @Valid PaymentPreviewRequestDTO requestDTO) {
        return ResponseEntity.ok(paymentService.preview(requestDTO));
    }

    @Operation(summary = "Listar pagamentos do paciente", description = """
            Retorna os pagamentos do paciente, ativos e inativos, do mais recente para o mais
            antigo; o contrato é um filtro opcional.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = PaymentResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> search(
            @Parameter(description = "Paciente dono da conta", required = true, example = "1") @RequestParam Long patientId,
            @Parameter(description = "Contrato (qualquer versão)", example = "1") @RequestParam(required = false) Long contractId) {
        return ResponseEntity.ok(paymentService.search(patientId, contractId));
    }

    @Operation(summary = "Inativar pagamento", description = """
            Inativa o pagamento (soft delete), para corrigir um registro errado.

            Regras: "Payment" no `docs/domain-model.md`.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pagamento inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O pagamento já está inativo", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do pagamento", required = true, example = "1") @PathVariable Long id) {
        paymentService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
