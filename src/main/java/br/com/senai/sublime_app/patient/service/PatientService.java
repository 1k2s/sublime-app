package br.com.senai.sublime_app.patient.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.sublime_app.patient.domain.Address;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.dto.AddressDTO;
import br.com.senai.sublime_app.patient.dto.PatientRequestDTO;
import br.com.senai.sublime_app.patient.dto.PatientResponseDTO;
import br.com.senai.sublime_app.patient.repository.PatientRepository;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import br.com.senai.sublime_app.shared.exception.ResourceNotFoundException;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Cadastra um novo paciente:
     * 1. Garante que o CPF ainda não está cadastrado.
     * 2. Cria a entidade pelo construtor de negócio (nasce ativa).
     * 3. Persiste e retorna o DTO de resposta.
     */
    @Transactional
    public PatientResponseDTO create(PatientRequestDTO dto) {
        if (patientRepository.existsByCpf(dto.cpf())) {
            throw new ConflictException("Já existe um paciente cadastrado com o CPF: " + dto.cpf());
        }

        PatientEntity patient = new PatientEntity(
                dto.name(),
                dto.cpf(),
                dto.birthDate(),
                dto.phone(),
                dto.email(),
                toAddress(dto.address()));

        patientRepository.save(patient);
        return PatientResponseDTO.fromEntity(patient);
    }

    /**
     * Lista todos os pacientes, ativos e inativos.
     */
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> findAll() {
        return patientRepository.findAll().stream()
                .map(PatientResponseDTO::fromEntity)
                .toList();
    }

    /**
     * Busca um paciente por ID, mesmo que esteja inativo (o histórico continua consultável).
     */
    @Transactional(readOnly = true)
    public PatientResponseDTO findById(Long id) {
        return PatientResponseDTO.fromEntity(getPatientOrThrow(id));
    }

    /**
     * Atualiza os dados do paciente pelos métodos de domínio da entidade.
     * O endereço (Value Object) só é substituído quando enviado; se vier nulo,
     * o endereço atual é preservado. A mudança é gravada pelo dirty checking do
     * Hibernate, pois a entidade foi carregada nesta transação.
     */
    @Transactional
    public PatientResponseDTO update(Long id, PatientRequestDTO dto) {
        PatientEntity patient = getPatientOrThrow(id);

        if (patientRepository.existsByCpfAndIdNot(dto.cpf(), id)) {
            throw new ConflictException("Já existe um paciente cadastrado com o CPF: " + dto.cpf());
        }

        patient.updatePersonalInfo(dto.name(), dto.cpf(), dto.birthDate());
        patient.updateContactInfo(dto.phone(), dto.email());

        if (dto.address() != null) {
            patient.updateAddress(toAddress(dto.address()));
        }

        return PatientResponseDTO.fromEntity(patient);
    }

    /**
     * Soft delete: o paciente é desativado, não removido do banco, para preservar
     * o histórico de contratos e atendimentos que o referenciam. Gravado pelo
     * dirty checking do Hibernate.
     */
    @Transactional
    public void deactivate(Long id) {
        PatientEntity patient = getPatientOrThrow(id);
        patient.deactivate();
    }

    // Centraliza a busca + erro de "não encontrado", usada por findById, update e deactivate
    private PatientEntity getPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado com id: " + id));
    }

    // Monta o Value Object a partir do DTO (o DTO de request não cria objeto de
    // domínio, como no pricing com Money.of). Endereço é opcional no cadastro.
    private Address toAddress(AddressDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Address(dto.street(), dto.numberHouse(), dto.city(), dto.complement(), dto.cep());
    }
}
