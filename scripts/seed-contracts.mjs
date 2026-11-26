// Dados de teste do módulo contract, criados pela API (nunca por INSERT: passam pelas regras
// do domínio) e conferidos na hora: cada chamada diz o status que espera e o script marca ✓ ou ✗.
// Cobre os casos da spec do aditivo (blocos A a D e F) e deixa os dados prontos para o bloco E
// (tela). Ver "Dados de teste" no docs/development.md.
//
// Uso (na raiz; MySQL e backend no ar, com o catálogo da migration V4):
//   node scripts/seed-contracts.mjs
//
// Pode rodar de novo: pacientes (CPF fixo) e contratos já criados são reaproveitados, e os
// aditivos e atendimentos só são feitos na primeira vez. Para recomeçar, zere o banco
// (`docker compose down -v`).
//
// Variáveis (opcionais): API_URL (padrão http://localhost:8080/api); ADMIN_EMAIL e
// ADMIN_PASSWORD (padrão: as do backend/.env).

import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

try {
  process.loadEnvFile(join(dirname(fileURLToPath(import.meta.url)), "..", "backend", ".env"));
} catch {
  // sem o arquivo: valem as variáveis de ambiente do sistema
}

const API_URL = process.env.API_URL ?? "http://localhost:8080/api";

// Prestador de teste (uso local): nasce com a senha provisória e já faz o primeiro acesso
const PROVIDER_EMAIL = "prestador.teste@sublime.com";
const PROVIDER_PROVISIONAL_PASSWORD = "provisoria123";
const PROVIDER_PASSWORD = "prestador123";

// "Hoje" no fuso da clínica (o backend usa o mesmo; ver ADR 0034)
const today = new Date().toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" });

function addDays(isoDate, days) {
  const date = new Date(`${isoDate}T00:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

// ---------------------------------------------------------------------------
// HTTP e conferências
// ---------------------------------------------------------------------------

let adminToken = null;

async function call(method, path, body, token = adminToken) {
  const response = await fetch(API_URL + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }
  return { status: response.status, data };
}

function describe({ data }) {
  if (data?.errors) return JSON.stringify(data.errors);
  return data?.detail ?? (typeof data === "string" ? data : JSON.stringify(data));
}

// Passo de preparação: se não vier o status esperado, para tudo (o resto depende dele)
async function must(method, path, body, expected = 200) {
  const res = await call(method, path, body);
  if (res.status !== expected) {
    throw new Error(`${method} ${path}: esperado ${expected}, veio ${res.status} (${describe(res)})`);
  }
  return res.data;
}

let passed = 0;
const failures = [];

function report(label, ok, detail) {
  if (ok) {
    passed++;
    console.log(`  ✓ ${label}${detail ? `  → ${detail}` : ""}`);
  } else {
    failures.push(label);
    console.log(`  ✗ ${label}${detail ? `\n      ${detail}` : ""}`);
  }
}

// Caso de regra: o status esperado e, se dado, um trecho da mensagem da API
function expectStatus(label, res, status, messagePart) {
  const message = describe(res);
  const ok = res.status === status && (!messagePart || message.includes(messagePart));
  report(label, ok, ok ? `${res.status} ${message}`.slice(0, 110) : `esperado ${status}${messagePart ? ` com "${messagePart}"` : ""}; veio ${res.status}: ${message}`);
}

function expectThat(label, condition, detail = "") {
  report(label, condition, condition ? "" : detail);
}

// ---------------------------------------------------------------------------
// Catálogo (já semeado pela V4): técnicas, planos e linhas de preço vigentes
// ---------------------------------------------------------------------------

const techniques = new Map();
const plans = new Map();
const priceTables = new Map();

async function loadCatalog() {
  for (const technique of await must("GET", "/techniques")) techniques.set(technique.name, technique);
  for (const plan of await must("GET", "/plans")) plans.set(plan.name, plan);
}

function technique(name) {
  if (!techniques.has(name)) throw new Error(`Técnica "${name}" não encontrada no catálogo.`);
  return techniques.get(name);
}

function plan(name) {
  if (!plans.has(name)) throw new Error(`Plano "${name}" não encontrado no catálogo.`);
  return plans.get(name);
}

async function priceLines(techniqueName) {
  if (!priceTables.has(techniqueName)) {
    const table = await must("GET", `/techniques/${technique(techniqueName).id}/prices`);
    priceTables.set(techniqueName, table.prices);
  }
  return priceTables.get(techniqueName);
}

// Item de grupo por duração: a linha vigente do plano e da duração
async function durationItem(techniqueName, planName, durationMinutes, frequency) {
  const line = (await priceLines(techniqueName)).find(
    (price) => price.planName === planName && price.durationMinutes === durationMinutes);
  if (!line) throw new Error(`Sem preço vigente: ${techniqueName} / ${planName} / ${durationMinutes} min.`);
  return { techniqueId: technique(techniqueName).id, sessionDurationPriceId: line.id, frequency };
}

// Item do Pilates em Grupo: a linha vigente do plano de mensalidade e da frequência semanal
async function groupItem(planName, weeklyFrequency) {
  const line = (await priceLines("Pilates em Grupo")).find(
    (price) => price.planName === planName && price.weeklyFrequency === weeklyFrequency);
  if (!line) throw new Error(`Sem preço vigente: Pilates em Grupo / ${planName} / ${weeklyFrequency}x.`);
  return { techniqueId: technique("Pilates em Grupo").id, sessionFrequencyPriceId: line.id, frequency: null };
}

// ---------------------------------------------------------------------------
// Pacientes e prestador
// ---------------------------------------------------------------------------

// CPF fixo (11 dígitos; o backend não confere os verificadores): serve para achar de novo
const PEOPLE = {
  ana: { name: "Ana Evolução", cpf: "90000000001", note: "contrato de pacote com 2 itens e 3 atendimentos" },
  bruno: { name: "Bruno Beneficiário", cpf: "90000000002", note: "livre: beneficiário nos aditivos" },
  caio: { name: "Caio Titular", cpf: "90000000003", note: "Pilates em Dupla, com a Débora de beneficiária" },
  debora: { name: "Débora Beneficiária", cpf: "90000000004", note: "beneficiária do contrato do Caio" },
  carla: { name: "Carla Trimestral", cpf: "90000000005", note: "só Pilates em Grupo (R6: incluir RPG no aditivo)" },
  elisa: { name: "Elisa Aditivos", cpf: "90000000006", note: "dois aditivos (histórico das versões)" },
  fabio: { name: "Fábio Vencido", cpf: "90000000007", note: "contrato ativo e vencido (prorrogar)" },
  gabriela: { name: "Gabriela Inativada", cpf: "90000000008", note: "contrato inativado" },
  hugo: { name: "Hugo Sem Contrato", cpf: "90000000009", note: "sem contrato: criar pela tela" },
  marina: { name: "Marina Mensalidade Mista", cpf: "90000000010", note: "R6: Trimestral + Grupo + RPG quinzenal" },
};

const patients = {};

async function ensurePatients() {
  const existing = await must("GET", "/patients");
  for (const [key, person] of Object.entries(PEOPLE)) {
    patients[key] =
      existing.find((patient) => patient.cpf === person.cpf) ??
      (await must("POST", "/patients", { name: person.name, cpf: person.cpf, birthDate: "1990-05-10" }, 201));
  }
}

async function ensureProvider() {
  const users = await must("GET", "/users");
  const user =
    users.find((candidate) => candidate.email === PROVIDER_EMAIL) ??
    (await must("POST", "/users", { email: PROVIDER_EMAIL, password: PROVIDER_PROVISIONAL_PASSWORD, role: "PROVIDER" }, 201));
  const providers = await must("GET", "/providers");
  return (
    providers.find((provider) => provider.userId === user.id) ??
    (await must("POST", "/providers", { userId: user.id, name: "Prestador de Teste" }, 201))
  );
}

// Token do prestador (para os casos de 403); faz o primeiro acesso se ainda estiver pendente
async function loginProvider() {
  const login = await call("POST", "/auth/login", { email: PROVIDER_EMAIL, password: PROVIDER_PASSWORD }, null);
  if (login.status === 200) return login.data.token;
  const firstAccess = await call(
    "POST",
    "/auth/first-access",
    { email: PROVIDER_EMAIL, provisionalPassword: PROVIDER_PROVISIONAL_PASSWORD, newPassword: PROVIDER_PASSWORD },
    null);
  return firstAccess.status === 200 ? firstAccess.data.token : null;
}

// ---------------------------------------------------------------------------
// Contratos
// ---------------------------------------------------------------------------

// A versão atual do contrato do titular (a listagem só traz as atuais), ou o contrato novo
async function ensureContract(personKey, buildRequest) {
  const patientId = patients[personKey].id;
  const contracts = await must("GET", "/contracts");
  const existing = contracts.filter((contract) => contract.patientId === patientId).sort((a, b) => b.id - a.id)[0];
  if (existing) return { contract: existing, created: false };
  return { contract: await must("POST", "/contracts", await buildRequest(), 201), created: true };
}

const toRequestItem = (item) => ({
  techniqueId: item.techniqueId,
  sessionDurationPriceId: item.sessionDurationPriceId,
  sessionFrequencyPriceId: item.sessionFrequencyPriceId,
  frequency: item.frequency,
});

// O aditivo leva o estado completo: o da versão atual, com o que muda por cima
const amendmentBody = (contract, changes = {}) => ({
  beneficiaryId: contract.beneficiaryId,
  paymentMethod: contract.paymentMethod,
  installments: contract.installments,
  items: contract.items.map(toRequestItem),
  ...changes,
});

const amend = (contract, changes) => must("POST", `/contracts/${contract.id}/amendments`, amendmentBody(contract, changes), 201);

// ---------------------------------------------------------------------------
// Execução
// ---------------------------------------------------------------------------

async function main() {
  const email = process.env.ADMIN_EMAIL;
  const password = process.env.ADMIN_PASSWORD;
  if (!email || !password) {
    throw new Error("Defina ADMIN_EMAIL e ADMIN_PASSWORD (ou preencha o backend/.env).");
  }

  console.log(`\nAPI: ${API_URL}  |  hoje: ${today}\n`);
  adminToken = (await must("POST", "/auth/login", { email, password }, 200)).token;

  await loadCatalog();
  await ensurePatients();
  const provider = await ensureProvider();
  const providerToken = await loginProvider();

  // -- 1. Dados -------------------------------------------------------------
  console.log("1. Dados");
  const rpgWeekly = () => durationItem("RPG", "Evolução", 60, "ONCE_A_WEEK");
  const quiroWeekly = () => durationItem("Quiropraxia", "Evolução", 60, "ONCE_A_WEEK");

  // Contrato 1 da spec: Evolução, 2 itens por duração, 1x por semana = 12 semanas, até 3 parcelas
  const ana = await ensureContract("ana", async () => ({
    patientId: patients.ana.id, planId: plan("Evolução").id, startDate: today,
    paymentMethod: "CREDIT_CARD", installments: 3, items: [await rpgWeekly(), await quiroWeekly()],
  }));
  if (ana.created) {
    // todos os status contam no saldo: RPG usa 2 de 12 e Quiropraxia 1 de 12
    for (const [techniqueName, status] of [["RPG", "ATTENDED"], ["RPG", "NO_SHOW"], ["Quiropraxia", "ATTENDED"]]) {
      await must("POST", "/consultations", {
        patientId: patients.ana.id, contractId: ana.contract.id, providerId: provider.id,
        techniqueId: technique(techniqueName).id, durationMinutes: 60, occurredAt: today, status,
      }, 201);
    }
  }
  console.log(`  Ana: contrato ${ana.contract.id} (${ana.created ? "criado, com 3 atendimentos" : "já existia"})`);

  // Pilates em Dupla com beneficiária (a Débora fica "ocupada" para o caso 409)
  const caio = await ensureContract("caio", async () => ({
    patientId: patients.caio.id, beneficiaryId: patients.debora.id, planId: plan("Transformação").id,
    startDate: today, paymentMethod: "PIX", installments: 3,
    items: [await durationItem("Pilates em Dupla", "Transformação", 60, "TWICE_A_WEEK")],
  }));
  console.log(`  Caio: contrato ${caio.contract.id} (${caio.created ? "criado" : "já existia"})`);

  // Contrato 2 da spec: só Pilates em Grupo (plano de mensalidade)
  const carla = await ensureContract("carla", async () => ({
    patientId: patients.carla.id, planId: plan("Trimestral").id, startDate: today,
    paymentMethod: "PIX", installments: 3, items: [await groupItem("Trimestral", 2)],
  }));
  console.log(`  Carla: contrato ${carla.contract.id} (${carla.created ? "criado" : "já existia"})`);

  // Elisa: original + 2 aditivos (1º: forma de pagamento e parcelas; 2º: prorrogação e parcelas)
  const elisa = await ensureContract("elisa", async () => ({
    patientId: patients.elisa.id, planId: plan("Evolução").id, startDate: today,
    paymentMethod: "PIX", installments: 3, items: [await rpgWeekly(), await groupItem("Trimestral", 2)],
  }));
  let elisaCurrent = elisa.contract;
  if (elisa.created) {
    const first = await amend(elisa.contract, { paymentMethod: "CREDIT_CARD", installments: 2 });
    const newEnd = addDays(first.endDate, 42);
    const preview = await must("POST", `/contracts/${first.id}/amendments/preview`, { endDate: newEnd, items: amendmentBody(first).items });
    elisaCurrent = await amend(first, { endDate: newEnd, installments: preview.maxInstallments });
  }
  console.log(`  Elisa: contrato ${elisaCurrent.id}, aditivo ${elisaCurrent.amendmentNumber}`);

  // Vencido: ativo, mas o fim (4 semanas) já passou
  const fabio = await ensureContract("fabio", async () => ({
    patientId: patients.fabio.id, planId: plan("Essencial").id, startDate: addDays(today, -70),
    paymentMethod: "PIX", installments: 1, items: [await durationItem("RPG", "Essencial", 60, "ONCE_A_WEEK")],
  }));
  console.log(`  Fábio: contrato ${fabio.contract.id} (${fabio.created ? "criado" : "já existia"})`);

  // Inativado (soft delete)
  const gabriela = await ensureContract("gabriela", async () => ({
    patientId: patients.gabriela.id, planId: plan("Essencial").id, startDate: today,
    paymentMethod: "PIX", installments: 1, items: [await durationItem("RPG", "Essencial", 60, "ONCE_A_WEEK")],
  }));
  if (gabriela.created) await must("DELETE", `/contracts/${gabriela.contract.id}`, undefined, 204);
  console.log(`  Gabriela: contrato ${gabriela.contract.id} (${gabriela.created ? "criado e inativado" : "já existia"})`);

  // R6: mensalidade com item por duração (RPG pela linha do Evolução, o equivalente do Trimestral)
  const marina = await ensureContract("marina", async () => ({
    patientId: patients.marina.id, planId: plan("Trimestral").id, startDate: today,
    paymentMethod: "PIX", installments: 6,
    items: [await groupItem("Trimestral", 2), await durationItem("RPG", "Evolução", 60, "BIWEEKLY")],
  }));
  console.log(`  Marina: contrato ${marina.contract.id} (${marina.created ? "criado" : "já existia"})`);

  // -- 2. Criação: regras recusadas (nada é gravado) -----------------------------
  console.log("\n2. Criação de contrato: regras");
  const post = (path, body) => call("POST", path, body);
  const hugo = patients.hugo.id;
  const base = { patientId: hugo, planId: plan("Evolução").id, startDate: today, paymentMethod: "PIX", installments: 3 };
  const rpg = await rpgWeekly();
  const group = await groupItem("Trimestral", 2);

  expectStatus("Plano Avulso não vira contrato",
    await post("/contracts", { ...base, planId: plan("Avulso").id, items: [await durationItem("RPG", "Avulso", 60, "ONCE_A_WEEK")] }), 400);
  expectStatus("Titular não pode ser o beneficiário",
    await post("/contracts", { ...base, beneficiaryId: hugo, items: [rpg] }), 400, "O beneficiário deve ser outro paciente.");
  expectStatus("Pacote sem item por duração",
    await post("/contracts", { ...base, items: [group] }), 400, "precisa de pelo menos um item por duração");
  expectStatus("Frequência acima do teto de 52 semanas (Vitalidade mensal)",
    await post("/contracts", { ...base, planId: plan("Vitalidade").id, items: [await durationItem("RPG", "Vitalidade", 60, "MONTHLY")] }), 400);
  expectStatus("Dois itens do mesmo grupo de preço",
    await post("/contracts", { ...base, items: [rpg, await durationItem("Liberação Miofascial", "Evolução", 60, "ONCE_A_WEEK")] }), 400);
  expectStatus("Parcelas acima dos meses do contrato (máximo 3)",
    await post("/contracts", { ...base, installments: 4, items: [rpg] }), 400, "O número de parcelas deve ser de 1 a 3");
  expectStatus("Técnica fora do grupo da linha de preço",
    await post("/contracts", { ...base, items: [{ ...rpg, techniqueId: technique("Quiropraxia").id }] }), 400);
  expectStatus("Titular com contrato vigente",
    await post("/contracts", { ...base, patientId: patients.ana.id, items: [rpg] }), 409, "O paciente já possui um contrato vigente.");
  expectStatus("Beneficiário em outro contrato vigente",
    await post("/contracts", { ...base, beneficiaryId: patients.debora.id, items: [rpg] }), 409, "O beneficiário já está em outro contrato vigente.");
  expectStatus("Mensalidade sem o item do Grupo (R6)",
    await post("/contracts", { ...base, planId: plan("Trimestral").id, items: [rpg] }), 400, "precisa do item do Pilates em Grupo");
  expectStatus("Mensalidade com item por duração de outro pacote (R6)",
    await post("/contracts", { ...base, planId: plan("Trimestral").id, items: [group, await durationItem("RPG", "Essencial", 60, "ONCE_A_WEEK")] }), 400, "precisa ser do plano Evolução");

  const preview = await post("/contracts/preview", {
    planId: plan("Evolução").id, startDate: today,
    items: [await durationItem("RPG", "Evolução", 60, "BIWEEKLY"), group],
  });
  expectStatus("Prévia: Evolução quinzenal + Grupo", preview, 200);
  expectThat("Prévia: 24 semanas e até 6 parcelas", preview.data?.durationWeeks === 24 && preview.data?.maxInstallments === 6,
    `veio ${preview.data?.durationWeeks} semanas e ${preview.data?.maxInstallments} parcelas`);

  // -- 3. Aditivo: regras recusadas (na versão atual da Ana; nada é gravado) -------
  console.log("\n3. Aditivo e prévia do aditivo: regras (contrato da Ana)");
  const anaContract = ana.contract;
  const anaAmendments = `/contracts/${anaContract.id}/amendments`;
  const { installments: _omitted, ...withoutInstallments } = amendmentBody(anaContract);

  expectStatus("A1: sem installments", await post(anaAmendments, withoutInstallments), 400, "O número de parcelas é obrigatório.");
  expectStatus("A2: installments 0", await post(anaAmendments, amendmentBody(anaContract, { installments: 0 })), 400);
  expectStatus("A2: installments 13", await post(anaAmendments, amendmentBody(anaContract, { installments: 13 })), 400);
  expectStatus("A3: parcelas acima dos meses da nova vigência (3)",
    await post(anaAmendments, amendmentBody(anaContract, { installments: 4 })), 400, "O número de parcelas deve ser de 1 a 3");
  expectStatus("A8: aditivo sem nenhuma alteração", await post(anaAmendments, amendmentBody(anaContract)), 400, "Nenhuma alteração no contrato.");

  const previewPath = `${anaAmendments}/preview`;
  const items = amendmentBody(anaContract).items;
  expectStatus("B1: prévia sem forma de pagamento nem beneficiário", await post(previewPath, { items }), 200);
  const cappedPreview = await post(previewPath, { items, installments: 12 });
  expectStatus("B2: prévia com parcelas acima do máximo", cappedPreview, 200);
  expectThat("B2: aplicadas = o máximo (3)", cappedPreview.data?.installments === 3 && cappedPreview.data?.maxInstallments === 3,
    `veio ${cappedPreview.data?.installments} de ${cappedPreview.data?.maxInstallments}`);
  expectStatus("B4: prévia com fim igual ao atual", await post(previewPath, { items, endDate: anaContract.endDate }), 400,
    "posterior ao fim atual");
  expectStatus("B8: prévia de contrato inexistente", await post("/contracts/999999999/amendments/preview", { items }), 404);
  const anaAfter = await must("GET", `/contracts/${anaContract.id}`);
  expectThat("B5: as prévias não gravaram nada (versão ativa, aditivo 0)", anaAfter.active && anaAfter.amendmentNumber === 0);
  expectStatus("D1: group-plan-options saiu", await call("GET", `/contracts/${anaContract.id}/group-plan-options`), 404);

  if (providerToken) {
    expectStatus("D2: PROVIDER na prévia do aditivo", await call("POST", previewPath, { items }, providerToken), 403);
    expectStatus("D2: PROVIDER em /versions", await call("GET", `/contracts/${anaContract.id}/versions`, undefined, providerToken), 403);
  } else {
    console.log("  - D2 pulado: não foi possível entrar com o prestador de teste");
  }

  // -- 4. Versões, status e invariantes --------------------------------------
  console.log("\n4. Versões, status e valores");
  const versions = await must("GET", `/contracts/${elisaCurrent.id}/versions`);
  expectThat("C2/C6: Elisa tem 3 versões, da mais nova à original (aditivo 2, 1 e 0)",
    versions.map((version) => version.amendmentNumber).join() === "2,1,0",
    `veio ${versions.map((version) => version.amendmentNumber).join()}`);
  expectThat("C6: só a atual não está substituída",
    versions.map((version) => version.superseded).join() === "false,true,true");
  const original = versions.at(-1);
  expectStatus("A7: aditivo na versão substituída", await post(`/contracts/${original.id}/amendments`, amendmentBody(elisaCurrent, { installments: 1 })),
    409, "Só a versão ativa do contrato pode ser alterada.");
  expectStatus("B6: prévia na versão substituída", await post(`/contracts/${original.id}/amendments/preview`, { items: amendmentBody(elisaCurrent).items }), 409);
  expectThat("C5: GET da versão substituída traz superseded", (await must("GET", `/contracts/${original.id}`)).superseded === true);

  const list = await must("GET", "/contracts");
  expectThat("C3: a listagem tem uma linha só da Elisa (a atual)",
    list.filter((contract) => contract.patientId === patients.elisa.id).length === 1);
  const gabrielaRow = list.find((contract) => contract.patientId === patients.gabriela.id);
  expectThat("C4: contrato inativado continua na listagem, sem superseded", gabrielaRow && !gabrielaRow.active && !gabrielaRow.superseded);
  const fabioRow = list.find((contract) => contract.patientId === patients.fabio.id);
  expectThat("Fábio: ativo e vencido (current falso)", fabioRow && fabioRow.active && !fabioRow.current);
  const marinaRow = list.find((contract) => contract.patientId === patients.marina.id);
  expectThat("R6: contrato da Marina dura 24 semanas (RPG quinzenal passa das 13 do Trimestral)",
    marinaRow && marinaRow.endDate === addDays(marinaRow.startDate, 24 * 7 - 1), `fim ${marinaRow?.endDate}`);

  const brokenSplits = list.filter((contract) => {
    const sum = contract.firstInstallmentValue + (contract.installments - 1) * contract.installmentValue;
    return Math.abs(sum - contract.totalValue) > 0.005;
  });
  expectThat("Parcelas somam o total (centavos na primeira) em todos os contratos", brokenSplits.length === 0,
    `contratos ${brokenSplits.map((contract) => contract.id).join(", ")}`);

  if (ana.created) {
    const balance = await must("GET", `/consultations/session-balance?contractId=${anaContract.id}`);
    const used = Object.fromEntries(balance.map((item) => [item.techniqueName, item.usedSessions]));
    expectThat("Saldo da Ana: RPG usou 2 e Quiropraxia 1 (todos os status contam)", used["RPG"] === 2 && used["Quiropraxia"] === 1,
      JSON.stringify(used));
  }

  // -- Resumo ---------------------------------------------------------------
  console.log(`\n${passed} conferências ok, ${failures.length} com falha.`);
  if (failures.length > 0) failures.forEach((label) => console.log(`  ✗ ${label}`));

  console.log("\nPacientes de teste (para a tela):");
  for (const person of Object.values(PEOPLE)) console.log(`  ${person.name.padEnd(26)} CPF ${person.cpf}  ${person.note}`);
  console.log(`\nLogin do prestador: ${PROVIDER_EMAIL} / ${PROVIDER_PASSWORD}`);

  process.exitCode = failures.length > 0 ? 1 : 0;
}

main().catch((error) => {
  console.error(`\nErro: ${error.message}`);
  process.exitCode = 1;
});
