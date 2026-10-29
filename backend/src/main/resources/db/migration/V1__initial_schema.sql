-- Schema inicial: as tabelas como o Hibernate as gerava (ddl-auto=update) até a
-- adoção do Flyway, revisadas à mão (ver ADR 0021):
-- - tabelas na ordem das dependências (quem é referenciado vem antes);
-- - constraints com nomes legíveis (pk/fk/uk/chk), no lugar dos nomes aleatórios do
--   Hibernate, para a mensagem de erro dizer qual regra foi violada;
-- - enums como varchar(30), SEM o CHECK de valores que o Hibernate cria: valor novo
--   num enum não exige migration (a validação dos valores fica no enum Java);
-- - collation utf8mb4_0900_ai_ci: a unicidade de nomes e e-mails ignora maiúsculas e
--   acentos ("Essencial" = "essencial").
-- Regras que o Hibernate não gera entram nas migrations seguintes (V2, V3).
-- Uma migration já aplicada NUNCA é editada: mudança nova = arquivo novo.

-- ---------------------------------------------------------------------------
-- user / provider
-- ---------------------------------------------------------------------------

CREATE TABLE `user` (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    active   BIT(1)       NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_user PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE provider (
    id                             BIGINT       NOT NULL AUTO_INCREMENT,
    active                         BIT(1)       NOT NULL,
    commission_percentage          DECIMAL(5,2) NOT NULL,
    name                           VARCHAR(100) NOT NULL,
    referral_commission_percentage DECIMAL(5,2) NOT NULL,
    user_id                        BIGINT       NOT NULL,
    CONSTRAINT pk_provider PRIMARY KEY (id),
    CONSTRAINT uk_provider_user UNIQUE (user_id),
    CONSTRAINT fk_provider_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- patient (endereço = Value Object embutido nas colunas cep..street)
-- ---------------------------------------------------------------------------

CREATE TABLE patient (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    active                BIT(1)       NOT NULL,
    cep                   VARCHAR(255) NULL,
    city                  VARCHAR(255) NULL,
    complement            VARCHAR(255) NULL,
    number_house          VARCHAR(255) NULL,
    street                VARCHAR(255) NULL,
    birth_date            DATE         NOT NULL,
    cpf                   VARCHAR(255) NOT NULL,
    email                 VARCHAR(255) NULL,
    name                  VARCHAR(255) NOT NULL,
    phone                 VARCHAR(255) NULL,
    referring_provider_id BIGINT       NULL,
    CONSTRAINT pk_patient PRIMARY KEY (id),
    CONSTRAINT uk_patient_cpf UNIQUE (cpf),
    CONSTRAINT fk_patient_referring_provider FOREIGN KEY (referring_provider_id) REFERENCES provider (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- pricing: grupos, técnicas, planos e preços
-- ---------------------------------------------------------------------------

CREATE TABLE pricing_group (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL,
    pricing_model VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_pricing_group PRIMARY KEY (id),
    CONSTRAINT uk_pricing_group_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE technique (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    active           BIT(1)       NOT NULL,
    name             VARCHAR(255) NOT NULL,
    pricing_group_id BIGINT       NOT NULL,
    CONSTRAINT pk_technique PRIMARY KEY (id),
    CONSTRAINT uk_technique_name UNIQUE (name),
    CONSTRAINT fk_technique_pricing_group FOREIGN KEY (pricing_group_id) REFERENCES pricing_group (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE plan (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    active             BIT(1)       NOT NULL,
    name               VARCHAR(255) NOT NULL,
    session_count      INT          NULL,
    term_weeks         INT          NULL,
    plan_type          VARCHAR(30)  NOT NULL,
    equivalent_plan_id BIGINT       NULL,
    CONSTRAINT pk_plan PRIMARY KEY (id),
    CONSTRAINT uk_plan_name UNIQUE (name),
    CONSTRAINT fk_plan_equivalent_plan FOREIGN KEY (equivalent_plan_id) REFERENCES plan (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE session_duration_price (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    duration_minutes INT           NOT NULL,
    session_value    DECIMAL(10,2) NOT NULL,
    valid_from       DATE          NOT NULL,
    valid_to         DATE          NULL,
    plan_id          BIGINT        NOT NULL,
    pricing_group_id BIGINT        NOT NULL,
    CONSTRAINT pk_session_duration_price PRIMARY KEY (id),
    CONSTRAINT fk_duration_price_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT fk_duration_price_pricing_group FOREIGN KEY (pricing_group_id) REFERENCES pricing_group (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE session_frequency_price (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    session_value    DECIMAL(10,2) NOT NULL,
    valid_from       DATE          NOT NULL,
    valid_to         DATE          NULL,
    weekly_frequency INT           NOT NULL,
    plan_id          BIGINT        NOT NULL,
    pricing_group_id BIGINT        NOT NULL,
    CONSTRAINT pk_session_frequency_price PRIMARY KEY (id),
    CONSTRAINT fk_frequency_price_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT fk_frequency_price_pricing_group FOREIGN KEY (pricing_group_id) REFERENCES pricing_group (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- contract (agregado: contract + contract_item)
-- ---------------------------------------------------------------------------

CREATE TABLE contract (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    active               BIT(1)      NOT NULL,
    end_date             DATE        NOT NULL,
    payment_method       VARCHAR(30) NOT NULL,
    start_date           DATE        NOT NULL,
    beneficiary_id       BIGINT      NULL,
    patient_id           BIGINT      NOT NULL,
    plan_id              BIGINT      NOT NULL,
    previous_contract_id BIGINT      NULL,
    CONSTRAINT pk_contract PRIMARY KEY (id),
    -- cada versão tem no máximo uma sucessora (aditivo)
    CONSTRAINT uk_contract_previous_contract UNIQUE (previous_contract_id),
    CONSTRAINT fk_contract_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_contract_beneficiary FOREIGN KEY (beneficiary_id) REFERENCES patient (id),
    CONSTRAINT fk_contract_plan FOREIGN KEY (plan_id) REFERENCES plan (id),
    CONSTRAINT fk_contract_previous_contract FOREIGN KEY (previous_contract_id) REFERENCES contract (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE contract_item (
    id                         BIGINT      NOT NULL AUTO_INCREMENT,
    frequency                  VARCHAR(30) NULL,
    contract_id                BIGINT      NOT NULL,
    session_duration_price_id  BIGINT      NULL,
    session_frequency_price_id BIGINT      NULL,
    technique_id               BIGINT      NOT NULL,
    CONSTRAINT pk_contract_item PRIMARY KEY (id),
    CONSTRAINT fk_contract_item_contract FOREIGN KEY (contract_id) REFERENCES contract (id),
    CONSTRAINT fk_contract_item_duration_price FOREIGN KEY (session_duration_price_id) REFERENCES session_duration_price (id),
    CONSTRAINT fk_contract_item_frequency_price FOREIGN KEY (session_frequency_price_id) REFERENCES session_frequency_price (id),
    CONSTRAINT fk_contract_item_technique FOREIGN KEY (technique_id) REFERENCES technique (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- consultation
-- ---------------------------------------------------------------------------

CREATE TABLE consultation (
    id                            BIGINT        NOT NULL AUTO_INCREMENT,
    active                        BIT(1)        NOT NULL,
    base_value                    DECIMAL(10,2) NOT NULL,
    commission_percentage_applied DECIMAL(5,2)  NOT NULL,
    duration_minutes              INT           NOT NULL,
    occurred_at                   DATE          NOT NULL,
    payout_value                  DECIMAL(10,2) NOT NULL,
    pricing_source                VARCHAR(30)   NOT NULL,
    status                        VARCHAR(30)   NOT NULL,
    contract_id                   BIGINT        NULL,
    patient_id                    BIGINT        NOT NULL,
    provider_id                   BIGINT        NOT NULL,
    session_duration_price_id     BIGINT        NULL,
    session_frequency_price_id    BIGINT        NULL,
    technique_id                  BIGINT        NOT NULL,
    CONSTRAINT pk_consultation PRIMARY KEY (id),
    CONSTRAINT fk_consultation_contract FOREIGN KEY (contract_id) REFERENCES contract (id),
    CONSTRAINT fk_consultation_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_consultation_provider FOREIGN KEY (provider_id) REFERENCES provider (id),
    CONSTRAINT fk_consultation_duration_price FOREIGN KEY (session_duration_price_id) REFERENCES session_duration_price (id),
    CONSTRAINT fk_consultation_frequency_price FOREIGN KEY (session_frequency_price_id) REFERENCES session_frequency_price (id),
    CONSTRAINT fk_consultation_technique FOREIGN KEY (technique_id) REFERENCES technique (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
