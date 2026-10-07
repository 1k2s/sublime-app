package br.com.senai.sublime_app.patient.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.patient.dto.PatientRequestDTO;
import br.com.senai.sublime_app.patient.dto.PatientResponseDTO;
import br.com.senai.sublime_app.patient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Pacientes", description = """
        Cadastro de pacientes da clínica. O CPF identifica o paciente e não pode se repetir.
        Pacientes nunca são removidos do banco: a exclusão é um soft delete (inativação).
        """)
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @Operation(summary = "Cadastrar paciente", description = """
            Cria um paciente ativo. O CPF deve ter exatamente 11 dígitos, sem pontuação.
            O endereço é opcional.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente criado", content = @Content(schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe um paciente com esse CPF", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PatientResponseDTO> create(@Valid @RequestBody PatientRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(dto));
    }

    @Operation(summary = "Listar pacientes", description = "Retorna todos os pacientes cadastrados, incluindo ativos e inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = PatientResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> findAll() {
        return ResponseEntity.ok(patientService.findAll());
    }

    @Operation(summary = "Buscar paciente por ID", description = "Retorna o paciente mesmo que esteja inativo (o histórico continua consultável).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado", content = @Content(schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> findById(
            @Parameter(description = "Identificador do paciente", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(patientService.findById(id));
    }

    @Operation(summary = "Atualizar paciente", description = """
            Atualiza dados pessoais e de contato do paciente.
            O endereço só é substituído quando enviado; se vier nulo, o endereço atual é mantido.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente atualizado", content = @Content(schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe outro paciente com esse CPF", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> update(
            @Parameter(description = "Identificador do paciente", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody PatientRequestDTO dto) {
        return ResponseEntity.ok(patientService.update(id, dto));
    }

    @Operation(summary = "Inativar paciente", description = """
            Realiza o **soft delete** do paciente: ele é marcado como inativo e não pode entrar
            em contratos novos, mas contratos e atendimentos antigos continuam apontando para ele.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O paciente já está inativo", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do paciente", required = true, example = "1") @PathVariable Long id) {
        patientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
