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
        Login e primeiro acesso. O token vai no header `Authorization: Bearer <token>`
        (no Swagger: "Authorize"). Regras: "User" no `docs/domain-model.md`.
        """)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = """
            Devolve o token (8 horas), a expiração e o usuário. Com senha provisória, responde
            **403** com `firstAccessRequired: true`, sem token: siga para
            `POST /api/auth/first-access`.
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
            Troca a senha provisória pela definitiva (8 caracteres ou mais, diferente da
            provisória) e já devolve o token.
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
