package br.com.senai.sublime_app.provider.controller;

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

import br.com.senai.sublime_app.provider.dto.ProviderRequestDTO;
import br.com.senai.sublime_app.provider.dto.ProviderResponseDTO;
import br.com.senai.sublime_app.provider.service.ProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;

    /**
     * Endpoint para cadastro de um novo prestador:
     * - @Valid valida os dados do ProviderRequestDTO (Bean Validation).
     * - @RequestBody recebe o JSON enviado e o converte para DTO.
     * - Retorna status HTTP 201 Created com o ProviderResponseDTO.
     */
    @PostMapping
    public ResponseEntity<ProviderResponseDTO> create(@Valid @RequestBody ProviderRequestDTO dto) {
        ProviderResponseDTO created = providerService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Endpoint para listagem de todos os prestadores:
     * - Retorna HTTP 200 OK com uma lista de ProviderResponseDTO.
     */
    @GetMapping
    public ResponseEntity<List<ProviderResponseDTO>> findAll() {
        return ResponseEntity.ok(providerService.findAll());
    }

    /**
     * Endpoint para buscar um prestador específico pelo ID:
     * - @PathVariable captura o ID informado na URL.
     * - Retorna HTTP 200 OK com o ProviderResponseDTO.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProviderResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.findById(id));
    }

    /**
     * Endpoint para atualizar os dados do prestador:
     * - Recebe o ID pela URL e os novos dados validados via DTO no corpo da requisição.
     * - Retorna HTTP 200 OK com o ProviderResponseDTO atualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProviderResponseDTO> update(@PathVariable Long id,
                                                      @Valid @RequestBody ProviderRequestDTO dto) {
        return ResponseEntity.ok(providerService.update(id, dto));
    }

    /**
     * Endpoint para exclusão de um prestador:
     * - Recebe o ID pela URL e remove o registro do banco.
     * - Retorna status HTTP 204 No Content (sem corpo de resposta).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        providerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
