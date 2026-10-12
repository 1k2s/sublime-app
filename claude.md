# Sublime Fisioterapia: guia completo do frontend
 
Consolida tudo o que foi definido e feito na conversa: decisões, dependências, estrutura de pastas, setup do projeto, passo a passo de implementação e estado atual.
 
## 1. Decisões fechadas
 
| Tema | Decisão |
|---|---|
| Linguagem | JavaScript (sem TypeScript) |
| Build | Vite, template `react` |
| Estilo | CSS Modules + variáveis CSS em `index.css` (sem Tailwind) |
| Repositório | Separado do backend: `sublime-fisioterapia-frontend` |
| Pasta dos módulos | `domain/` (não `features/`) |
| Idioma | Código (classes, atributos, funções, pastas) em inglês; comentários e documentação em português |
| Time | Todos fazem de tudo, com divisão parcial por módulo |
 
### Por que repositório separado
 
O backend é um projeto Spring Boot com Maven (`pom.xml`, `mvnw`, `src`, `docs`), com 1 commit e 4 branches. Para não reestruturar o backend nem arriscar as branches abertas, o frontend fica em um repositório próprio.
 
| Perda | Como compensar |
|---|---|
| Mudança de API e tela em um PR só | Dois PRs ligados, citando um ao outro. O do backend entra primeiro, sem quebrar a compatibilidade |
| `docs/domain-model.md` só no backend | Manter lá como fonte única e colocar o link no README do front |
| DTOs duplicados à mão | OpenAPI do backend (springdoc-openapi) como contrato |
| Convenções em um lado só | `CLAUDE.md` no repositório do front com idioma, estrutura e regra de dependência |
 
Combinados mínimos:
1. Nomes padronizados: `sublime-fisioterapia-backend` e `sublime-fisioterapia-frontend`.
2. Contrato da API exposto pelo backend e lido antes de implementar cada tela.
3. Mesmo nome de branch nos dois repositórios quando a funcionalidade cruza ambos (por exemplo, `feat/consultation-create`).
4. CORS liberado no Spring só em desenvolvimento, para `http://localhost:5173`.
5. README do front com o link do backend, a variável `VITE_API_URL` e como subir os dois lados.
 
Reavaliar o monorepo se o vai-e-vem entre dois PRs começar a atrapalhar. A unificação posterior é possível com `git subtree`, preservando o histórico.
 
## 2. Dependências
 
```bash
npm create vite@latest sublime-frontend -- --template react
cd sublime-frontend
 
npm i react-router-dom lucide-react react-pro-sidebar
npm i @tanstack/react-query react-hook-form zod @hookform/resolvers
npm i axios date-fns decimal.js
```
 
| Dependência | Para que serve |
|---|---|
| `react-router-dom` | Rotas |
| `lucide-react` | Ícones |
| `react-pro-sidebar` | Menu lateral (traz o `@emotion/styled` junto) |
| `@tanstack/react-query` | Cache e invalidação das chamadas à API |
| `react-hook-form` | Formulários |
| `zod` | Validação |
| `@hookform/resolvers` | Liga o Zod ao React Hook Form |
| `axios` | Cliente HTTP (token e tratamento de 401) |
| `date-fns` | Datas (`validFrom`/`validTo`, filtros por período) |
| `decimal.js` | Cálculo preciso com valores monetários |
 
Total: 10 dependências de runtime. O template do Vite já traz `react`, `react-dom`, `vite`, `@vitejs/plugin-react` e ESLint.
 
Pontos de atenção:
- Confirmar no npm se a versão do `react-pro-sidebar` é compatível com a versão do React instalada (a manutenção dela é irregular).
- Sem TypeScript, o Zod e o JSDoc compensam a falta de tipos.
- Alternativas descartadas por ora: Tailwind, shadcn/ui, router e ícones próprios.
 
## 3. Estrutura de pastas
 
```
src/
├── main.jsx
├── index.css                      # variáveis de cor e estilo global
│
├── app/                           # casca da aplicação
│   ├── providers.jsx              # QueryClientProvider, AuthProvider
│   ├── router.jsx                 # rotas + rotas protegidas
│   └── layout/
│       ├── AppLayout.jsx
│       ├── AppSidebar.jsx
│       ├── Topbar.jsx
│       └── navigationItems.js
│
├── shared/                        # sem regra de negócio
│   ├── http/
│   │   └── httpClient.js          # axios: token e 401
│   ├── utils/
│   │   ├── money.js               # formatCurrency (Decimal + Intl)
│   │   ├── date.js                # formatDate (date-fns)
│   │   └── masks.js               # CPF, telefone, CEP
│   └── ui/                        # Button, Card, Input, Select, FormField,
│                                  # Stepper, Badge, PageHeader, Table, Modal
│                                  # (cada um com seu .module.css)
│
└── domain/                        # um diretório por módulo do backend
    ├── patient/                   # nível 1
    ├── pricing/                   # nível 1
    ├── auth/                      # nível 1: sessão, login e proteção de rota (+ context/)
    ├── user/                      # nível 1: gestão de usuários
    ├── provider/                  # nível 2 (depende de user)
    ├── contract/                  # nível 2 (depende de patient + pricing)
    └── consultation/              # nível 3 (depende de todos)
```
 
Dentro de cada domínio: `api/`, `components/`, `schemas/`, `pages/`.
Extras em `consultation`: `hooks/`, `components/wizard/` e `consultationStatus.js`.
 
### Conteúdo por domínio
 
| Domínio | Principais arquivos |
|---|---|
| `patient` | `PatientForm`, `AddressFields`, `PatientTable`, `PatientSearchInput`, `patientSchema.js` |
| `pricing` | `TechniqueTable`, `PlanTable`, `PriceHistoryTable`, `PriceAdjustmentForm`, `PricingCatalogPage` |
| `auth` | `LoginForm`, `ProtectedRoute`, `AuthContext`, `useAuth`, `loginSchema.js`, `LoginPage`. Exceção à regra de um diretório por módulo do backend: a sessão atravessa o app inteiro, então fica separada de `user` |
| `user` | Gestão de usuários (cadastro e contas). Ainda sem arquivos |
| `provider` | `ProviderForm` (inclui percentual de comissão), `ProviderTable` |
| `contract` | `ContractForm`, `PriceSelector`, `ContractTable`, `contractSchema.js` (exclusive arc) |
| `consultation` | `ConsultationWizard`, `PatientStep`, `ConfirmationStep`, `SuccessStep`, `ConsultationPreview`, `ConsultationTable`, `ConsultationFilters`, `StatusBadge`, `useConsultationWizard`, `consultationStatus.js` |
 
### Onde fica cada coisa
 
| Local | O que vai lá |
|---|---|
| `domain/<modulo>/pages/` | Telas inteiras, uma por rota. Buscam dados e montam componentes |
| `domain/<modulo>/components/` | Peças que pertencem a um módulo. Recebem dados por props |
| `shared/ui/` | Peças genéricas, sem regra de negócio |
| `app/layout/` | Sidebar, topbar e layout geral |
 
### Regras de dependência
 
- `consultation` importa de `contract`, `provider`, `patient`, `pricing` e `user`. Nunca o contrário.
- `shared/` nunca importa de `domain/`.
- A camada `app/` pode importar de qualquer domínio.
- Usado em um domínio só: `domain/<modulo>/components/`. Genérico e sem regra de negócio: `shared/ui/`.
 
### Onde guardar cada tipo de estado
 
| Tipo de dado | Exemplo | Onde fica |
|---|---|---|
| Vem da API | Pacientes, contratos, preços, lançamentos | React Query (hooks `useXxx` em `domain/<modulo>/api/`) |
| Estado global do app, que não vem de listagem | Sessão do usuário, tema | Contexto, com o provider em `app/providers.jsx` |
| Estado de uma tela ou fluxo | Passo atual do wizard de lançamento | `useState` ou um hook local (por exemplo, `useConsultationWizard`) |
 
- Dados do servidor nunca vão em contexto: o cache do React Query já compartilha a mesma lista entre os componentes, com carregamento, erro e invalidação.
- Todo provider global é aninhado no `AppProviders` (`app/providers.jsx`); o `main.jsx` não monta providers direto. Quem fica por dentro pode usar quem está por fora.
- Um provider que serve a uma tela só é montado em volta dela, e não no `AppProviders`.
 
### Convenções de nomes
 
- Componentes e pages: `PascalCase.jsx`.
- Hooks: começam com `use`.
- Demais arquivos: `camelCase.js`.
 
## 4. O que vai em `domain/<modulo>/api/`
 
Dois tipos de arquivo:
 
| Arquivo | Conteúdo | Conhece |
|---|---|---|
| `xxxApi.js` | Funções que chamam o backend, uma por endpoint | `httpClient` |
| `useXxx.js` | Hooks do TanStack Query | `xxxApi.js` e o React Query |
 
### Camada
 
```
api (xxxApi.js)  →  hook (useXxx.js)  →  components / pages
```
 
| Camada | O que faz | O que não sabe |
|---|---|---|
| API | Faz a chamada e devolve os dados | Que existe React |
| Hook | Transforma a chamada em estado de tela (carregando, erro, dados) | Como a tela é desenhada |
| Components / pages | Mostram os dados e disparam as ações | De onde os dados vêm |
 
Cada camada só conhece a anterior. Trocar um mock pelo endpoint real mexe só na camada de API.
 
- Leitura usa `useQuery`: roda quando o componente aparece e guarda o resultado em cache.
- Ação que altera algo (login, criar, editar, apagar) usa `useMutation`: só roda quando a tela chama `mutate`.
 
```js
// domain/patient/api/patientApi.js
// Chamadas HTTP do módulo patient (uma função por endpoint)
import { httpClient } from "@/shared/http/httpClient";
 
export async function listPatients(search) {
  const { data } = await httpClient.get("/patients", { params: { search } });
  return data;
}
 
export async function createPatient(payload) {
  const { data } = await httpClient.post("/patients", payload);
  return data;
}
```
 
```js
// domain/patient/api/usePatients.js
// Hooks de leitura (com cache) e escrita (com invalidação)
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { listPatients, createPatient } from "./patientApi";
 
export function usePatients(search) {
  return useQuery({
    queryKey: ["patients", search],
    queryFn: () => listPatients(search),
  });
}
 
export function useCreatePatient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: createPatient,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["patients"] }),
  });
}
```
 
Regras:
- Pages e components usam só os hooks, nunca o `httpClient` direto.
- Sem JSX e sem regra de negócio nessa pasta.
- Padronizar as `queryKey` por módulo (`["patients", ...]`, `["consultations", ...]`).
- Valores monetários trafegam como string e só são formatados em `shared/utils/money.js`.
- Com o backend atrasado, o `xxxApi.js` pode devolver dados mockados sem afetar o resto.
 
## 5. Tela de Lançamentos (referência do design)
 
A tela tem três camadas, cada uma em um lugar:
 
| Trecho da tela | Componente | Local |
|---|---|---|
| Menu lateral e barra superior | `AppSidebar`, `Topbar` | `app/layout/` |
| Indicador 1 → 2 → 3 | `Stepper` | `shared/ui/` |
| Busca de paciente por nome ou CPF | `PatientSearchInput` | `domain/patient/components/` |
| Passos 1, 2 e 3 | `PatientStep`, `ConfirmationStep`, `SuccessStep` | `domain/consultation/components/wizard/` |
| Painel "Prévia do lançamento" | `ConsultationPreview` | `domain/consultation/components/` |
| Estado compartilhado entre passos e prévia | `useConsultationWizard` | `domain/consultation/hooks/` |
 
O front envia só `patientId` e `arrivalTime`. O prestador vem do usuário logado, e o backend resolve o contrato e grava o snapshot financeiro (`baseValue`, comissão e repasse). O passo 2 só exibe o resultado para conferência.
 
## 6. Setup do projeto, passo a passo
 
### 6.1 Criar e testar
 
```bash
npm create vite@latest sublime-frontend -- --template react
cd sublime-frontend
npm install
npm run dev
```
 
Se a página do template aparecer, está tudo certo.
 
### 6.2 Instalar as dependências
 
Use os comandos da seção 2.
 
### 6.3 Alias `@/`
 
O alias é um atalho que aponta para `src/`. Evita imports como `../../../../shared/ui/Button` e deixa a regra de dependência visível na revisão (`@/domain/patient/...`).
 
```js
// vite.config.js
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath, URL } from "node:url";
 
export default defineConfig({
  plugins: [react()],
  resolve: { alias: { "@": fileURLToPath(new URL("./src", import.meta.url)) } },
});
```
 
```json
// jsconfig.json (na raiz, nome exato; reinicie o VS Code depois de criar)
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": { "@/*": ["./src/*"] }
  },
  "include": ["src"]
}
```
 
| Arquivo | Função |
|---|---|
| `vite.config.js` | Faz o projeto compilar e rodar com `@/` |
| `jsconfig.json` | Faz o editor reconhecer o `@/` (autocomplete e "ir para definição") |
 
### 6.4 Variável de ambiente
 
Na raiz, crie o `.env` (reinicie o `npm run dev` depois de alterar):
 
```
VITE_API_URL=http://localhost:8080/api
```
 
### 6.5 Criar as pastas
 
**Git Bash, WSL, Linux ou macOS** (a partir da raiz do projeto):
 
```bash
cd src
 
# Limpar o template
rm -rf assets App.css App.jsx
 
# Camadas base
mkdir -p app/layout
mkdir -p shared/{http,ui,utils}
 
# Pasta de cada domínio
for d in patient pricing user provider contract consultation; do
  mkdir -p domain/$d/{api,components,schemas,pages}
done
 
# Extras
mkdir -p domain/user/context
mkdir -p domain/consultation/{hooks,components/wizard}
 
cd ..
```
 
**PowerShell:**
 
```powershell
cd src
Remove-Item -Recurse -Force assets, App.css, App.jsx
New-Item -ItemType Directory -Force app/layout, shared/http, shared/ui, shared/utils
foreach ($d in "patient","pricing","user","provider","contract","consultation") {
  foreach ($s in "api","components","schemas","pages") {
    New-Item -ItemType Directory -Force "domain/$d/$s"
  }
}
New-Item -ItemType Directory -Force domain/user/context, domain/consultation/hooks, domain/consultation/components/wizard
cd ..
```
 
O `mkdir -p` ignora pastas que já existem. O Git não versiona pastas vazias; para commitar antes de criar arquivos, use `find src -type d -empty -exec touch {}/.gitkeep \;`.
 
### 6.6 Estilo global
 
```css
/* src/index.css */
:root {
  --color-bg: #faf7f0;
  --color-surface: #ffffff;
  --color-primary: #5f7a58;
  --color-primary-soft: #dfe8dc;
  --color-border: #e5e0d5;
  --color-text: #374151;
  --color-muted: #6b7280;
  --radius: 12px;
}
 
* { box-sizing: border-box; }
 
body {
  margin: 0;
  background: var(--color-bg);
  color: var(--color-text);
  font-family: system-ui, sans-serif;
}
```
 
### 6.7 `main.jsx` mínimo
 
Como o `App.jsx` é apagado, o `main.jsx` original quebra. Use este para validar React Query e Router:
 
```jsx
// src/main.jsx
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./index.css";
 
const queryClient = new QueryClient();
 
createRoot(document.getElementById("root")).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <h1>Sublime Fisioterapia</h1>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>
);
```
 
Rode `npm run dev`. Se o título aparecer sobre fundo creme, o setup está pronto.
 
### 6.8 Versionar
 
```bash
git init
git add .
git commit -m "chore: initial frontend setup"
git branch -M main
git remote add origin <url-do-repositorio>
git push -u origin main
```
 
Confira se o `.gitignore` inclui `node_modules` e `dist` (e o `.env`, se tiver dado sensível).
 
## 7. Passo a passo de implementação
 
1. **Criar o projeto e instalar** as dependências.
2. **Configurar o ambiente:** alias `@/`, `jsconfig.json`, `.env` e CORS no Spring.
3. **Estilo global e pastas:** variáveis CSS, limpar o template e criar a árvore.
4. **Camada `shared/`:** `httpClient`, `money`, `date`, `masks` e os componentes de `ui/` usados pela tela de Lançamentos (`Card`, `Button`, `FormField`, `Input`, `Select`, `Stepper`, `Badge`, `PageHeader`).
5. **Camada `app/`:** providers, router, `AppLayout`, sidebar e topbar. Marco: a casca abre com menu e página vazia.
6. **Módulo `auth`:** login, `AuthContext`, `ProtectedRoute` (pode começar com usuário mock).
7. **Módulos `patient` e `pricing`:** api, hooks, schema, componentes e pages. Criar já o `PatientSearchInput`.
8. **Módulos `provider` e `contract`:** o contrato valida o exclusive arc (exatamente um dos dois preços preenchido).
9. **Módulo `consultation`:** status, api, wizard de 3 passos, prévia e a página de Lançamentos.
10. **Fechar a Fase 1:** histórico com filtros, lint, build e checklist final.
 
Dica: fazer um módulo por vez, com a tela funcionando de ponta a ponta, em vez de todas as APIs e depois todas as telas.
 
### Checklist final da Fase 1
 
- [ ] Login funcionando e rotas protegidas.
- [ ] CRUD de `Patient` com endereço e ViaCEP.
- [ ] Catálogo de `pricing` consultável.
- [ ] Cadastro de `provider` e `contract`.
- [ ] Fluxo completo de lançamento (3 passos + prévia).
- [ ] Histórico de atendimentos com filtro por período.
- [ ] Valores monetários sempre como string, formatados só na exibição.
- [ ] Nada com nome em português no código.
- [ ] `npm run lint` sem erros e `npm run build` gerando o bundle.
 
## 8. Estado atual do projeto
 
Projeto `sublime-app-front` criado e rodando no Vite. Atualizado em 2026-10-12. O projeto está no passo 6 da seção 7: tela de login pronta, autenticação pendente.
 
| Item | Situação |
|---|---|
| `jsconfig.json`, `.env`, `vite.config.js` | Criados na raiz, alias `@/` configurado |
| Dependências da seção 2 | Todas instaladas (React 19, React Router 7, Zod 4) |
| `main.jsx` | Pronto. Usa `RouterProvider` (data router) no lugar do `BrowserRouter` de 6.7, com o `QueryClient` criado inline |
| `index.css` | Variáveis de 6.6 aplicadas, mais `--color-accent` (barra do item ativo do menu) |
| `app/router.jsx` | Pronto com `createBrowserRouter`, rotas `lazy` e imports com `@/`: `/login`, `/patients`, `/pricing`, `/providers`, `/contracts`, `/consultations` e `*`. A rota `/` redireciona para `/consultations`. Ainda sem `ProtectedRoute` |
| `app/layout/` | `AppLayout` (sidebar + conteúdo com rolagem própria), `AppSidebar` (`react-pro-sidebar` + `NavLink`) e `navigationItems.js` prontos. Faltam `Topbar` e `providers.jsx` |
| Menu lateral | Mostra a união dos menus de admin e prestador. Cada item tem `roles` em `navigationItems.js`, mas o filtro por papel ainda não é aplicado. Dashboard e Configurações estão desabilitados (sem tela). Logo em texto, e usuário e logout fixos até existir o `AuthContext` |
| `shared/http/httpClient.js` | Pronto: token do `localStorage` (`sublime.authToken`), tratamento de 401 e `setUnauthorizedHandler` à espera do `AuthContext` |
| `shared/utils/` | `money.js`, `date.js` e `masks.js` prontos |
| `shared/ui/` | `Button`, `Input` e `FormField` prontos, com `.module.css`. `PageLoader` e `RouteError` ainda usam estilo inline. Faltam `Card`, `Select`, `Stepper`, `Badge` e `PageHeader` |
| `domain/auth/` | Tela de login pronta (`LoginPage`, `LoginForm`, `loginSchema.js`), só com visual e validação: o envio válido apenas navega para `/`. Faltam `AuthContext`, `useAuth`, `ProtectedRoute` e a API de login |
| `domain/<modulo>/pages/` | Uma page placeholder em `patient`, `pricing`, `provider`, `contract` e `consultation`. Faltam `api/`, `components/` e `schemas/` em todos. `domain/user/` ainda não tem arquivos |
| `src/pages/NotFoundPage.jsx` | Existe fora da estrutura da seção 3. Decidir para onde mover (por exemplo, `app/`) |
| `assets/`, `App.css`, `App.jsx` do template | Apagados. Resta `public/icons.svg`, sem uso |
| `README.md` | Ainda é o do template do Vite |
 
## 9. Pontos de atenção do modelo
 
- **Nomenclatura:** `repasseValue` está em português. Renomear no backend e no front para algo como `providerPayoutValue`.
- **"Agendamentos"** aparece no menu do design, mas não existe no mapa de módulos do backend. Deixar o item desabilitado até definir.
- **Dashboard e Relatórios** são Fase 2. Manter só o item de menu, desabilitado.
- **Valores monetários** sempre como string vinda da API. O cálculo oficial fica no backend (`BigDecimal`).
- **Preços historizados:** nunca editar. O reajuste fecha a linha antiga (`validTo`) e abre uma nova.
- **Exclusive arc** (`groupPlanPriceId` / `groupPlanFrequencyPriceId`): validado no schema Zod do front, com a `CHECK` constraint no banco como garantia final.
 
## 10. Próximos passos
 
- [x] Trocar `main.jsx` e `index.css` e validar com `npm run dev`.
- [x] Escrever `httpClient.js`, `money.js`, `date.js` e `masks.js`.
- [x] Configurar o data router com rotas `lazy`.
- [x] Criar o `CLAUDE.md`.
- [x] Limpar o template (apagar `assets/`, `App.css`, `App.jsx`).
- [x] Ajustar o `router.jsx`: imports com `@/`, rota index em `/` e `Outlet` removido das pages folha.
- [x] Remover a centralização de `#root` no `index.css`.
- [x] Montar `AppLayout`, `AppSidebar` e `navigationItems.js`.
- [ ] Criar as subpastas que faltam nos domínios.
- [ ] Sidebar: trocar a logo em texto pelo arquivo da marca e filtrar os itens por papel (`roles`) quando houver usuário logado.
- [ ] Definir as telas de Dashboard e Configurações e o lugar de Prestadores no menu (a rota `/providers` existe, mas não está no design).
- [ ] Mover o `NotFoundPage` para dentro da estrutura da seção 3.
- [x] Criar `Button`, `Input` e `FormField` em `shared/ui/` e a tela de login em `domain/auth/`.
- [ ] Criar os demais componentes de `shared/ui/` conforme as telas precisarem e migrar `PageLoader` e `RouteError` para CSS Modules.
- [ ] Completar a casca (`providers.jsx` e `Topbar`, se o design tiver barra superior).
- [ ] Módulo `auth`, etapa 2: `AuthContext` + `useAuth`, `providers.jsx` na raiz, API de login (mock), `ProtectedRoute` no router e ligação do `setUnauthorizedHandler`.
- [ ] Escrever o README do repositório do front.
- [ ] Alinhar com o time o contrato OpenAPI e a convenção de branches.