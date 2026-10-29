-- Só UMA linha de preço vigente (valid_to nulo) por combinação de grupo + plano +
-- eixo (duração ou frequência). O código já checa antes de criar ("já existe linha
-- vigente?"), mas duas requisições simultâneas passariam as duas pela checagem e
-- criariam dois preços vigentes. O UNIQUE é verificado pelo próprio banco no momento
-- da gravação, então a segunda é recusada (409).
--
-- Como funciona:
-- - current_flag é uma coluna CALCULADA pelo banco (ninguém a preenche): vale 1 se a
--   linha está vigente (valid_to nulo) e NULL se foi fechada por reajuste. É
--   recalculada no mesmo comando que altera valid_to. STORED = o valor fica gravado,
--   o que permite usá-la num índice.
-- - O UNIQUE não aceita duas linhas com os mesmos valores nas quatro colunas. Como o
--   MySQL não considera um NULL igual a outro, as linhas fechadas (NULL) podem se
--   repetir à vontade (histórico de reajustes); só o 1 (vigente) não pode repetir.
-- - TINYINT: o menor inteiro; a coluna só guarda 1 ou NULL. Não pode ser 0 nas
--   fechadas: duas linhas com 0 contariam como repetidas e bloqueariam o histórico.
--
-- Consequência no código: no reajuste, a linha antiga precisa ser fechada no banco
-- ANTES do INSERT da nova (PricingService usa saveAndFlush), senão as duas teriam
-- current_flag = 1 por um instante.

ALTER TABLE session_duration_price
    ADD COLUMN current_flag TINYINT AS (IF(valid_to IS NULL, 1, NULL)) STORED,
    ADD CONSTRAINT uk_duration_price_current
        UNIQUE (pricing_group_id, plan_id, duration_minutes, current_flag);

ALTER TABLE session_frequency_price
    ADD COLUMN current_flag TINYINT AS (IF(valid_to IS NULL, 1, NULL)) STORED,
    ADD CONSTRAINT uk_frequency_price_current
        UNIQUE (pricing_group_id, plan_id, weekly_frequency, current_flag);
