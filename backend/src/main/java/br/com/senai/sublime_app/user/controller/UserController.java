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
        Cadastro dos usuários de acesso ao sistema (perfil `ADMIN` ou `PROVIDER`).
        O e-mail não pode se repetir. Usuários nunca são removidos do banco: a exclusão
        é um soft delete (inativação).
        """)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cadastrar usuário", description = """
            Cria um usuário ativo. A senha precisa ter no mínimo 8 caracteres, é gravada com
            hash e nunca é devolvida nas respostas. É **provisória**: no primeiro login, o
            usuário define a própria senha (`POST /api/auth/first-access`).
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
            O usuário logado troca o próprio e-mail e, se informar `newPassword`, a senha
            (mínimo de 8 caracteres; vazia ou ausente mantém a atual). Sempre exige a **senha
            atual** (`currentPassword`). O perfil não muda por aqui. Trocar o e-mail não encerra
            a sessão: o token continua valendo.

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
            Atualiza e-mail e perfil (obrigatórios). A **senha é opcional**: vazia ou ausente
            mantém a atual; preenchida, vira uma nova senha **provisória** (mínimo de 8
            caracteres): a sessão aberta do usuário cai, e no próximo login ele define a
            própria senha.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe outro usuário com esse e-mail, ou tentativa de mudar o perfil do administrador padrão", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @Operation(summary = "Inativar usuário", description = """
            Realiza o **soft delete** do usuário: ele é marcado como inativo e preservado no
            banco, junto com o prestador vinculado a ele (se houver).
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuário inativado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "O usuário já está inativo, ou é o administrador padrão (não pode ser inativado)", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @Parameter(description = "Identificador do usuário", required = true, example = "1") @PathVariable Long id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reativar usuário", description = """
            Desfaz a inativação: o usuário volta a poder fazer login. Os dados do cadastro não
            mudam.
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
