-- Exclusive arc garantido pelo banco: item de contrato e atendimento referenciam
-- EXATAMENTE uma linha de preço, por duração OU por frequência — nunca as duas, nunca
-- nenhuma. O código já valida (ContractItemDraft, ConsultationEntity); o CHECK é a
-- última barreira para qualquer caminho que escape dele (ex: um INSERT manual).
--
-- (a IS NULL) <> (b IS NULL) só é verdadeiro quando exatamente uma das duas é nula:
--   duração=5,    frequência=NULL → TRUE <> FALSE → aceita
--   duração=NULL, frequência=3    → FALSE <> TRUE → aceita
--   duração=5,    frequência=3    → FALSE <> FALSE → recusa
--   duração=NULL, frequência=NULL → TRUE <> TRUE   → recusa

ALTER TABLE contract_item
    ADD CONSTRAINT chk_contract_item_one_price
        CHECK ((session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL));

ALTER TABLE consultation
    ADD CONSTRAINT chk_consultation_one_price
        CHECK ((session_duration_price_id IS NULL) <> (session_frequency_price_id IS NULL));
