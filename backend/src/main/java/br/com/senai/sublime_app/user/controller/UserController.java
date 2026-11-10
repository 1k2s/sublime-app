package br.com.senai.sublime_app.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.user.domain.CurrentUser;
import br.com.senai.sublime_app.user.dto.UserRequestDTO;
import br.com.senai.sublime_app.user.dto.UserResponseDTO;
import br.com.senai.sublime_app.user.dto.UserSelfUpdateRequestDTO;
import br.com.senai.sublime_app.user.dto.UserUpdateRequestDTO;
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
        Usuários de acesso (`ADMIN` ou `PROVIDER`). Regras: "User" no `docs/domain-model.md`.
        """)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cadastrar usuário", description = """
            Senha com 8 caracteres ou mais, **provisória**: no primeiro login, o usuário define
            a própria (`POST /api/auth/first-access`).
            """)
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

    @Operation(summary = "Meus dados (Configurações)", description = """
            Retorna o usuário logado (identificado pelo token).

            **Perfis:** ADMIN e PROVIDER.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário logado", content = @Content(schema = @Schema(implementation = UserResponseDTO.class)))
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> findMe(CurrentUser currentUser) {
        return ResponseEntity.ok(userService.findMe(currentUser));
    }

    @Operation(summary = "Alterar meus dados (Configurações)", description = """
            Troca o próprio e-mail e, com `newPassword`, a senha. Exige a `currentPassword`.

            **Perfis:** ADMIN e PROVIDER.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados atualizados", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou senha atual incorreta", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe outro usuário com esse e-mail", content = @Content)
    })
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateMe(@Valid @RequestBody UserSelfUpdateRequestDTO dto,
            CurrentUser currentUser) {
        return ResponseEntity.ok(userService.updateMe(currentUser, dto));
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

    @Operation(summary = "Atualizar usuário", description = """
            E-mail e perfil obrigatórios. `password` opcional: preenchida, vira uma nova senha
            **provisória** (a sessão do usuário cai).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "E-mail já usado, ou mudança de perfil do admin padrão", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @Operation(summary = "Inativar usuário", description = """
            Soft delete: o usuário não faz mais login (a sessão cai na hora).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já inativo, ou é o admin padrão", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reativar usuário", description = """
            Desfaz a inativação: o usuário volta a fazer login.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário reativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O usuário já está ativo", content = @Content)
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activate(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id) {
        userService.activate(id);
        return ResponseEntity.noContent().build();
    }
}
