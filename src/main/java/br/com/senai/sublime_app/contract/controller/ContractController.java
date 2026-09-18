package br.com.senai.sublime_app.contract.controller;

import br.com.senai.sublime_app.contract.dto.ContractRequestDTO;
import br.com.senai.sublime_app.contract.dto.ContractResponseDTO;
import br.com.senai.sublime_app.contract.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /**
     * Cria um novo contrato.
     * Valida que o paciente não possui contrato ativo e trava o preço vigente.
     *
     * @param requestDTO dados do contrato com as FKs de preço (exclusive arc)
     * @return contrato criado com status 201
     */
    @PostMapping
    public ResponseEntity<ContractResponseDTO> create(@RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos os contratos.
     *
     * @return lista de contratos com status 200
     */
    @GetMapping
    public ResponseEntity<List<ContractResponseDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    /**
     * Busca um contrato pelo ID.
     *
     * @param id identificador do contrato
     * @return contrato encontrado com status 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }

    /**
     * Atualiza os dados de negociação de um contrato (frequência, datas, pagamento).
     * Os campos de preço são imutáveis após a assinatura.
     *
     * @param id         identificador do contrato
     * @param requestDTO novos dados de negociação
     * @return contrato atualizado com status 200
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContractResponseDTO> update(@PathVariable Long id,
                                                      @RequestBody @Valid ContractRequestDTO requestDTO) {
        ContractResponseDTO response = contractService.update(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * Inativa um contrato (soft delete).
     *
     * @param id identificador do contrato
     * @return status 204 sem corpo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ResponseEntity.noContent().build();
    }
}