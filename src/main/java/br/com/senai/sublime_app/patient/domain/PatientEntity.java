package br.com.senai.sublime_app.patient.domain;

import java.time.LocalDate;

import br.com.senai.sublime_app.provider.domain.ProviderEntity;
import br.com.senai.sublime_app.shared.exception.BusinessRuleException;
import br.com.senai.sublime_app.shared.exception.ConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "patient")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PatientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    private String phone;

    private String email;

    @Embedded
    private Address address;

    // Origem: prestador que trouxe o paciente (null = paciente da clínica). Define o
    // percentual de repasse dos atendimentos. Ver domain-model.md, "Origem do paciente".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referring_provider_id")
    private ProviderEntity referringProvider;

    @Column(nullable = false)
    private boolean active;

    // Construtor de negócio: todo paciente novo nasce ativo e sem id (gerado pelo banco)
    public PatientEntity(String name, String cpf, LocalDate birthDate, String phone, String email,
            Address address, ProviderEntity referringProvider) {
        validatePersonalInfo(name, cpf, birthDate);
        validateReferringProvider(referringProvider, null);
        this.name = name;
        this.cpf = cpf;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.referringProvider = referringProvider;
        this.active = true;
    }

    // Dados cadastrais básicos, atualizados juntos no fluxo de edição do paciente
    public void updatePersonalInfo(String name, String cpf, LocalDate birthDate) {
        validatePersonalInfo(name, cpf, birthDate);
        this.name = name;
        this.cpf = cpf;
        this.birthDate = birthDate;
    }

    // Substitui telefone e e-mail juntos, em vez de dois setters soltos: os dois
    // costumam ser atualizados no mesmo fluxo de "atualizar contato" do paciente.
    public void updateContactInfo(String phone, String email) {
        this.phone = phone;
        this.email = email;
    }

    // Endereço é Value Object: sempre substituído por inteiro, nunca editado
    // campo a campo (ver Address).
    public void updateAddress(Address newAddress) {
        this.address = newAddress;
    }

    // Troca a origem (ex: corrigir um cadastro). null = paciente da clínica. Os
    // atendimentos já lançados não mudam: o percentual aplicado é snapshot.
    public void updateReferringProvider(ProviderEntity newReferringProvider) {
        validateReferringProvider(newReferringProvider, this.referringProvider);
        this.referringProvider = newReferringProvider;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        requireActive();
        this.active = false;
    }

    // Inativar duas vezes não é uma operação válida: 409 avisa que o estado já era esse
    private void requireActive() {
        if (!active) {
            throw new ConflictException("O paciente já está inativo.");
        }
    }

    // Invariantes do paciente, válidas para qualquer caminho que crie ou edite o
    // cadastro. O CPF é a identidade do paciente (base da checagem de duplicidade),
    // por isso o formato é garantido aqui e não só no DTO.
    private static void validatePersonalInfo(String name, String cpf, LocalDate birthDate) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("O nome do paciente é obrigatório.");
        }
        if (cpf == null || !cpf.matches("\\d{11}")) {
            throw new BusinessRuleException("O CPF deve conter exatamente 11 dígitos, sem pontuação.");
        }
        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data de nascimento é obrigatória e não pode ser futura.");
        }
    }

    // Prestador atribuído como origem precisa estar ativo; manter o mesmo de antes é
    // permitido mesmo que ele tenha sido inativado depois (o que é novo precisa ser
    // válido — mesma ideia do aditivo de contrato). Compara ids: relação LAZY pode vir
    // como proxy do Hibernate.
    private static void validateReferringProvider(ProviderEntity newProvider, ProviderEntity currentProvider) {
        if (newProvider == null) {
            return;
        }
        boolean sameAsCurrent = currentProvider != null && currentProvider.getId().equals(newProvider.getId());
        if (!newProvider.isActive() && !sameAsCurrent) {
            throw new BusinessRuleException("O prestador informado como origem do paciente está inativo.");
        }
    }
}
