-- Seed do catálogo de preços atual da clínica (módulo pricing inteiro):
--   1. grupos de preço;
--   2. técnicas;
--   3. planos;
--   4. de-para dos planos;
--   5. preços vigentes (por duração e por frequência).
-- Fontes: "Grupos atuais da clínica" e "Planos atuais da clínica" no
-- docs/domain-model.md, e "Catálogo atual da clínica" no docs/development.md.
--
-- Por que migration e não ApplicationRunner (como o AdminInitializer): o admin é um
-- problema de bootstrap (sem ele ninguém faz login); o catálogo não é. São dados de
-- referência versionados junto com o schema, aplicados uma única vez e registrados
-- no flyway_schema_history.
--
-- Idempotente com bancos locais que já tinham esses cadastros feitos pela API:
-- - grupos, técnicas e planos só entram se o nome ainda não existe (a collation ai_ci
--   ignora maiúsculas e acentos), então a migration não quebra nos uk_*_name;
-- - o de-para só é preenchido onde ainda está vazio;
-- - um preço só entra se a combinação ainda não tem linha vigente (o mesmo que o
--   uk_*_price_current da V3 exige).
-- Nada que a clínica já cadastrou é sobrescrito.
--
-- Depois do seed, o catálogo muda só pela aplicação (reajuste = fecha a linha vigente e
-- cria outra; ver ADR 0003). Esta migration NUNCA é editada para refletir reajustes.

-- ===========================================================================
-- 1. grupos de preço
-- ===========================================================================

INSERT INTO pricing_group (name, pricing_model)
SELECT seed.name, seed.pricing_model
FROM (
    SELECT 'Individual Padrão'        AS name, 'DURATION_BASED'  AS pricing_model UNION ALL
    SELECT 'Individual Especializado',          'DURATION_BASED'                  UNION ALL
    SELECT 'Em Dupla',                          'DURATION_BASED'                  UNION ALL
    SELECT 'Em Grupo',                          'FREQUENCY_BASED'
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM pricing_group g WHERE g.name = seed.name);

-- ===========================================================================
-- 2. técnicas (cada uma no grupo que define o preço dela; o grupo nunca muda)
-- ===========================================================================

INSERT INTO technique (active, name, pricing_group_id)
SELECT b'1', seed.name, g.id
FROM (
    SELECT 'Liberação Miofascial'   AS name, 'Individual Padrão'        AS group_name UNION ALL
    SELECT 'RPG',                            'Individual Padrão'                      UNION ALL
    SELECT 'Massoterapia',                   'Individual Padrão'                      UNION ALL
    SELECT 'Pilates Individual',             'Individual Padrão'                      UNION ALL
    SELECT 'Quiropraxia',                    'Individual Especializado'               UNION ALL
    SELECT 'Reabilitação Vestibular',        'Individual Especializado'               UNION ALL
    SELECT 'Pilates em Dupla',               'Em Dupla'                               UNION ALL
    SELECT 'Pilates em Grupo',               'Em Grupo'
) AS seed
JOIN pricing_group g ON g.name = seed.group_name
WHERE NOT EXISTS (SELECT 1 FROM technique t WHERE t.name = seed.name);

-- ===========================================================================
-- 3. planos (coerência do PlanType: SINGLE_SESSION/PACKAGE com session_count e sem
--    term_weeks; MONTHLY_FEE com term_weeks e sem session_count)
-- ===========================================================================

INSERT INTO plan (active, name, plan_type, session_count, term_weeks)
SELECT b'1', seed.name, seed.plan_type, seed.session_count, seed.term_weeks
FROM (
    SELECT 'Avulso'                 AS name, 'SINGLE_SESSION' AS plan_type, 1    AS session_count, NULL AS term_weeks UNION ALL
    SELECT 'Essencial',                      'PACKAGE',                     4,                     NULL               UNION ALL
    SELECT 'Evolução',                       'PACKAGE',                     12,                    NULL               UNION ALL
    SELECT 'Evolução Familiar',              'PACKAGE',                     12,                    NULL               UNION ALL
    SELECT 'Transformação',                  'PACKAGE',                     26,                    NULL               UNION ALL
    SELECT 'Transformação Familiar',         'PACKAGE',                     26,                    NULL               UNION ALL
    SELECT 'Vitalidade',                     'PACKAGE',                     52,                    NULL               UNION ALL
    SELECT 'Vitalidade Familiar',            'PACKAGE',                     52,                    NULL               UNION ALL
    SELECT 'Mensal',                         'MONTHLY_FEE',                 NULL,                  4                  UNION ALL
    SELECT 'Trimestral',                     'MONTHLY_FEE',                 NULL,                  13                 UNION ALL
    SELECT 'Semestral',                      'MONTHLY_FEE',                 NULL,                  26                 UNION ALL
    SELECT 'Semestral Familiar',             'MONTHLY_FEE',                 NULL,                  26                 UNION ALL
    SELECT 'Anual',                          'MONTHLY_FEE',                 NULL,                  52                 UNION ALL
    SELECT 'Anual Familiar',                 'MONTHLY_FEE',                 NULL,                  52
) AS seed
WHERE NOT EXISTS (SELECT 1 FROM plan p WHERE p.name = seed.name);

-- ===========================================================================
-- 4. de-para (equivalent_plan_id), nos dois lados (ADR 0007). Assimetria:
--    Evolução Familiar → Trimestral, mas o Trimestral volta para o Evolução.
--    Avulso não tem equivalente.
-- ===========================================================================

UPDATE plan p
JOIN (
    SELECT 'Essencial'              AS name, 'Mensal'                 AS equivalent UNION ALL
    SELECT 'Evolução',                       'Trimestral'                           UNION ALL
    SELECT 'Evolução Familiar',              'Trimestral'                           UNION ALL
    SELECT 'Transformação',                  'Semestral'                            UNION ALL
    SELECT 'Transformação Familiar',         'Semestral Familiar'                   UNION ALL
    SELECT 'Vitalidade',                     'Anual'                                UNION ALL
    SELECT 'Vitalidade Familiar',            'Anual Familiar'                       UNION ALL
    SELECT 'Mensal',                         'Essencial'                            UNION ALL
    SELECT 'Trimestral',                     'Evolução'                             UNION ALL
    SELECT 'Semestral',                      'Transformação'                        UNION ALL
    SELECT 'Semestral Familiar',             'Transformação Familiar'               UNION ALL
    SELECT 'Anual',                          'Vitalidade'                           UNION ALL
    SELECT 'Anual Familiar',                 'Vitalidade Familiar'
) AS seed ON seed.name = p.name
JOIN plan e ON e.name = seed.equivalent
SET p.equivalent_plan_id = e.id
WHERE p.equivalent_plan_id IS NULL;

-- ===========================================================================
-- 5a. preços por duração (grupos DURATION_BASED; planos SINGLE_SESSION/PACKAGE)
--     valid_from = dia em que a migration roda, valid_to nulo = vigente.
--     Evolução Familiar no Individual Especializado e no Em Dupla: valor ainda
--     não definido pela clínica — fica de fora, cadastrar pela tela de preço.
-- ===========================================================================

INSERT INTO session_duration_price (duration_minutes, session_value, valid_from, valid_to, plan_id, pricing_group_id)
SELECT seed.duration_minutes, seed.session_value, CURRENT_DATE, NULL, p.id, g.id
FROM (
    -- Individual Padrão
    SELECT 'Individual Padrão' AS group_name, 'Avulso' AS plan_name, 60 AS duration_minutes, 220.00 AS session_value UNION ALL
    SELECT 'Individual Padrão', 'Avulso',                 30, 132.00 UNION ALL
    SELECT 'Individual Padrão', 'Essencial',              60, 180.00 UNION ALL
    SELECT 'Individual Padrão', 'Essencial',              30, 113.00 UNION ALL
    SELECT 'Individual Padrão', 'Evolução',               60, 167.00 UNION ALL
    SELECT 'Individual Padrão', 'Evolução',               30, 100.00 UNION ALL
    SELECT 'Individual Padrão', 'Evolução Familiar',      60, 156.00 UNION ALL
    SELECT 'Individual Padrão', 'Evolução Familiar',      30,  93.00 UNION ALL
    SELECT 'Individual Padrão', 'Transformação',          60, 143.00 UNION ALL
    SELECT 'Individual Padrão', 'Transformação',          30,  90.00 UNION ALL
    SELECT 'Individual Padrão', 'Transformação Familiar', 60, 137.00 UNION ALL
    SELECT 'Individual Padrão', 'Transformação Familiar', 30,  85.00 UNION ALL
    SELECT 'Individual Padrão', 'Vitalidade',             60, 134.00 UNION ALL
    SELECT 'Individual Padrão', 'Vitalidade',             30,  80.00 UNION ALL
    SELECT 'Individual Padrão', 'Vitalidade Familiar',    60, 129.00 UNION ALL
    SELECT 'Individual Padrão', 'Vitalidade Familiar',    30,  77.00 UNION ALL
    -- Individual Especializado
    SELECT 'Individual Especializado', 'Avulso',                 60, 275.00 UNION ALL
    SELECT 'Individual Especializado', 'Avulso',                 30, 176.00 UNION ALL
    SELECT 'Individual Especializado', 'Essencial',              60, 242.00 UNION ALL
    SELECT 'Individual Especializado', 'Essencial',              30, 140.80 UNION ALL
    SELECT 'Individual Especializado', 'Evolução',               60, 231.00 UNION ALL
    SELECT 'Individual Especializado', 'Evolução',               30, 132.00 UNION ALL
    SELECT 'Individual Especializado', 'Transformação',          60, 186.00 UNION ALL
    SELECT 'Individual Especializado', 'Transformação',          30, 117.00 UNION ALL
    SELECT 'Individual Especializado', 'Transformação Familiar', 60, 178.00 UNION ALL
    SELECT 'Individual Especializado', 'Transformação Familiar', 30, 112.00 UNION ALL
    SELECT 'Individual Especializado', 'Vitalidade',             60, 175.00 UNION ALL
    SELECT 'Individual Especializado', 'Vitalidade',             30, 105.00 UNION ALL
    SELECT 'Individual Especializado', 'Vitalidade Familiar',    60, 170.00 UNION ALL
    SELECT 'Individual Especializado', 'Vitalidade Familiar',    30, 100.00 UNION ALL
    -- Em Dupla
    SELECT 'Em Dupla', 'Avulso',                 60, 265.00 UNION ALL
    SELECT 'Em Dupla', 'Avulso',                 30, 158.00 UNION ALL
    SELECT 'Em Dupla', 'Essencial',              60, 238.00 UNION ALL
    SELECT 'Em Dupla', 'Essencial',              30, 143.00 UNION ALL
    SELECT 'Em Dupla', 'Evolução',               60, 225.00 UNION ALL
    SELECT 'Em Dupla', 'Evolução',               30, 136.00 UNION ALL
    SELECT 'Em Dupla', 'Transformação',          60, 215.00 UNION ALL
    SELECT 'Em Dupla', 'Transformação',          30, 129.00 UNION ALL
    SELECT 'Em Dupla', 'Transformação Familiar', 60, 205.00 UNION ALL
    SELECT 'Em Dupla', 'Transformação Familiar', 30, 126.00 UNION ALL
    SELECT 'Em Dupla', 'Vitalidade',             60, 200.00 UNION ALL
    SELECT 'Em Dupla', 'Vitalidade',             30, 124.00 UNION ALL
    SELECT 'Em Dupla', 'Vitalidade Familiar',    60, 195.00 UNION ALL
    SELECT 'Em Dupla', 'Vitalidade Familiar',    30, 118.00
) AS seed
JOIN pricing_group g ON g.name = seed.group_name AND g.pricing_model = 'DURATION_BASED'
JOIN plan p ON p.name = seed.plan_name AND p.plan_type IN ('SINGLE_SESSION', 'PACKAGE')
WHERE NOT EXISTS (
    SELECT 1 FROM session_duration_price x
    WHERE x.pricing_group_id = g.id
      AND x.plan_id = p.id
      AND x.duration_minutes = seed.duration_minutes
      AND x.valid_to IS NULL
);

-- ===========================================================================
-- 5b. preços por frequência (grupo Em Grupo; planos MONTHLY_FEE) — valor por aula.
--     O valor mensal da planilha não é gravado: só o valor por aula é linha de preço.
-- ===========================================================================

INSERT INTO session_frequency_price (weekly_frequency, session_value, valid_from, valid_to, plan_id, pricing_group_id)
SELECT seed.weekly_frequency, seed.session_value, CURRENT_DATE, NULL, p.id, g.id
FROM (
    SELECT 'Mensal' AS plan_name, 1 AS weekly_frequency, 82.50 AS session_value UNION ALL
    SELECT 'Mensal',             2, 73.63 UNION ALL
    SELECT 'Mensal',             3, 58.91 UNION ALL
    SELECT 'Trimestral',         1, 73.36 UNION ALL
    SELECT 'Trimestral',         2, 65.81 UNION ALL
    SELECT 'Trimestral',         3, 52.50 UNION ALL
    SELECT 'Semestral',          1, 70.06 UNION ALL
    SELECT 'Semestral',          2, 62.57 UNION ALL
    SELECT 'Semestral',          3, 51.06 UNION ALL
    SELECT 'Semestral Familiar', 1, 67.27 UNION ALL
    SELECT 'Semestral Familiar', 2, 60.41 UNION ALL
    SELECT 'Semestral Familiar', 3, 49.98 UNION ALL
    SELECT 'Anual',              1, 63.46 UNION ALL
    SELECT 'Anual',              2, 55.85 UNION ALL
    SELECT 'Anual',              3, 48.91 UNION ALL
    SELECT 'Anual Familiar',     1, 58.38 UNION ALL
    SELECT 'Anual Familiar',     2, 54.58 UNION ALL
    SELECT 'Anual Familiar',     3, 47.83
) AS seed
JOIN pricing_group g ON g.name = 'Em Grupo' AND g.pricing_model = 'FREQUENCY_BASED'
JOIN plan p ON p.name = seed.plan_name AND p.plan_type = 'MONTHLY_FEE'
WHERE NOT EXISTS (
    SELECT 1 FROM session_frequency_price x
    WHERE x.pricing_group_id = g.id
      AND x.plan_id = p.id
      AND x.weekly_frequency = seed.weekly_frequency
      AND x.valid_to IS NULL
);
