package br.com.senai.sublime_app.patient.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.senai.sublime_app.patient.domain.Address;
import br.com.senai.sublime_app.patient.domain.PatientEntity;
import br.com.senai.sublime_app.patient.dto.PatientRequestDTO;
import br.com.senai.sublime_app.patient.dto.PatientResponseDTO;
import br.com.senai.sublime_app.patient.repository.PatientRepository;

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
    public PatientResponseDTO create(PatientRequestDTO dto) {
        if (patientRepository.existsByCpf(dto.getCpf())) {
            throw new RuntimeException("A patient is already registered with cpf: " + dto.getCpf());
        }

        PatientEntity patient = new PatientEntity(
                dto.getName(),
                dto.getCpf(),
                dto.getBirthDate(),
                dto.getPhone(),
                dto.getEmail(),
                toAddress(dto));

        return new PatientResponseDTO(patientRepository.save(patient));
    }

    /**
     * Lista todos os pacientes, ativos e inativos.
     */
    public List<PatientResponseDTO> findAll() {
        return patientRepository.findAll()
                .stream()
                .map(PatientResponseDTO::new)
                .toList();
    }

    /**
     * Busca um paciente por ID, mesmo que esteja inativo (o histórico continua consultável).
     */
    public PatientResponseDTO findById(Long id) {
        return new PatientResponseDTO(getPatientOrThrow(id));
    }

    /**
     * Atualiza os dados do paciente pelos métodos de domínio da entidade.
     * O endereço (Value Object) só é substituído quando enviado; se vier nulo,
     * o endereço atual é preservado.
     */
    public PatientResponseDTO update(Long id, PatientRequestDTO dto) {
        PatientEntity patient = getPatientOrThrow(id);

        if (patientRepository.existsByCpfAndIdNot(dto.getCpf(), id)) {
            throw new RuntimeException("A patient is already registered with cpf: " + dto.getCpf());
        }

        patient.updatePersonalInfo(dto.getName(), dto.getCpf(), dto.getBirthDate());
        patient.updateContactInfo(dto.getPhone(), dto.getEmail());

        if (dto.getAddress() != null) {
            patient.updateAddress(dto.getAddress().toValueObject());
        }

        return new PatientResponseDTO(patientRepository.save(patient));
    }

    /**
     * Soft delete: o paciente é desativado, não removido do banco, para preservar
     * o histórico de contratos e atendimentos que o referenciam.
     */
    public void deactivate(Long id) {
        PatientEntity patient = getPatientOrThrow(id);
        patient.deactivate();
        patientRepository.save(patient);
    }

    // Centraliza a busca + erro de "não encontrado", usada por findById, update e deactivate
    private PatientEntity getPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
    }

    // Endereço é opcional no cadastro
    private Address toAddress(PatientRequestDTO dto) {
        return dto.getAddress() != null ? dto.getAddress().toValueObject() : null;
    }
}
