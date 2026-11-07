package br.com.senai.sublime_app.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.senai.sublime_app.auth.dto.FirstAccessRequestDTO;
import br.com.senai.sublime_app.auth.dto.LoginRequestDTO;
import br.com.senai.sublime_app.auth.dto.LoginResponseDTO;
import br.com.senai.sublime_app.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = """
        Login por e-mail e senha e definição obrigatória de senha pessoal no primeiro acesso.
        O token devolvido vai em todas as outras chamadas, no header
        `Authorization: Bearer <token>` (no Swagger: botão "Authorize").
        """)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = """
            Autentica um usuário **ativo** e devolve o token JWT, a data de expiração (o token
            vale 8 horas, sem renovação: venceu, faz login de novo) e os dados do usuário.

            Se o usuário estiver com senha provisória de primeiro acesso e acertar a senha,
            responde **403** com `firstAccessRequired: true` (sem emitir token) para que a tela
            solicite a definição da nova senha pessoal em `POST /api/auth/first-access`.

            Por segurança, e-mail inexistente, senha errada e usuário inativo devolvem a
            **mesma** mensagem. Inativar o usuário ou mudar o perfil dele invalida o token na
            hora.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos", content = @Content),
            @ApiResponse(responseCode = "403", description = "Primeiro acesso pendente (troca de senha obrigatória)", content = @Content)
    })
    @SecurityRequirements // vazio: rota pública
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(summary = "Definir senha no primeiro acesso", description = """
            Troca a senha provisória definida pelo administrador por uma senha pessoal e
            definitiva do próprio titular (mínimo de 8 caracteres e diferente da senha
            provisória), devolvendo o token JWT para iniciar a sessão imediatamente.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha definida e login realizado", content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou nova senha igual à provisória", content = @Content),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos", content = @Content)
    })
    @SecurityRequirements // vazio: rota pública
    @PostMapping("/first-access")
    public ResponseEntity<LoginResponseDTO> completeFirstAccess(@Valid @RequestBody FirstAccessRequestDTO dto) {
        return ResponseEntity.ok(authService.completeFirstAccess(dto));
    }
}
