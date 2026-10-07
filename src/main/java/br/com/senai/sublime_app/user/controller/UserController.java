package br.com.senai.sublime_app.user.controller;

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

import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.dto.UserResponseDTO;
import br.com.senai.sublime_app.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = """
        Cadastro dos usuários de acesso ao sistema (perfil `ADMIN` ou `PROVIDER`).
        O e-mail não pode se repetir. Usuários nunca são removidos do banco: a exclusão
        é um soft delete (inativação).
        """)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cadastrar usuário", description = "Cria um usuário ativo. A senha nunca é devolvida nas respostas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário criado", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe um usuário com esse e-mail", content = @Content)
    })
    @PostMapping
    public ResponseEntity<UserResponseDTO> create(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(dto));
    }

    @Operation(summary = "Listar usuários", description = "Retorna todos os usuários cadastrados, incluindo ativos e inativos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso", content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @Operation(summary = "Buscar usuário por ID", description = "Retorna o usuário mesmo que esteja inativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário encontrado", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza e-mail, senha e perfil. Todos os campos são obrigatórios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe outro usuário com esse e-mail", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @Operation(summary = "Inativar usuário", description = """
            Realiza o **soft delete** do usuário: ele é marcado como inativo e preservado no
            banco, junto com o prestador vinculado a ele (se houver).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
