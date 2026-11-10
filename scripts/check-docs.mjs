// Verificação da documentação, rodada ao concluir uma tarefa (ver "Ao concluir uma tarefa"
// no CLAUDE.md). Uso, na raiz do repositório:
//
//   node scripts/check-docs.mjs
//
// Confere:
//   1. âncoras: todo link "#..." (no mesmo arquivo ou em outro .md) aponta para um título
//      que existe, com a âncora calculada como o GitHub calcula;
//   2. links para arquivos e pastas: o destino existe;
//   3. specs: toda menção a "specs/<nome>.md" (docs e código) aponta para uma spec que
//      existe, e nenhum comentário de código cita spec (cite um ADR ou um documento de
//      referência; a spec some quando a feature termina).
// Ignora o conteúdo de blocos de código (```). Sai com código 1 se houver problema.
// Sem dependências: só o Node.

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const DOCS_DIRS = ["docs"];
const ROOT_DOCS = ["README.md", "CLAUDE.md"];
const CODE_DIRS = ["backend/src", "frontend/src"];
const CODE_EXTENSIONS = new Set([".java", ".js", ".jsx", ".sql", ".xml", ".properties", ".css"]);
const IGNORED_DIRS = new Set(["node_modules", "target", "dist", ".git"]);

const problems = [];
let linksChecked = 0;

function walk(dir, accept) {
  const absolute = path.join(ROOT, dir);
  if (!fs.existsSync(absolute)) return [];
  const files = [];
  for (const entry of fs.readdirSync(absolute, { withFileTypes: true })) {
    if (IGNORED_DIRS.has(entry.name)) continue;
    const relative = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...walk(relative, accept));
    else if (accept(entry.name)) files.push(relative);
  }
  return files;
}

// Linhas fora de blocos de código (```), com o número original de cada uma
function proseLines(text) {
  const lines = [];
  let inFence = false;
  text.split(/\r?\n/).forEach((line, index) => {
    if (/^\s*(```|~~~)/.test(line)) {
      inFence = !inFence;
      return;
    }
    if (!inFence) lines.push({ line, number: index + 1 });
  });
  return lines;
}

// Âncora de um título, como o GitHub gera (github-slugger): minúsculas, sem pontuação
// (letras, números, "-" e "_" ficam), espaços viram "-"
function slug(heading) {
  return heading
    .replace(/<!--.*?-->/g, "")
    .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/[`*]/g, "")
    .trim()
    .toLowerCase()
    .replace(/[^\p{L}\p{M}\p{N}\p{Pc} -]/gu, "")
    .replace(/ /g, "-");
}

const anchorCache = new Map();

function anchorsOf(file) {
  if (anchorCache.has(file)) return anchorCache.get(file);
  const anchors = new Set();
  const seen = new Map();
  for (const { line } of proseLines(fs.readFileSync(path.join(ROOT, file), "utf8"))) {
    const match = line.match(/^#{1,6}\s+(.*?)\s*#*\s*$/);
    if (!match) continue;
    const base = slug(match[1]);
    const count = seen.get(base) ?? 0;
    anchors.add(count === 0 ? base : `${base}-${count}`);
    seen.set(base, count + 1);
  }
  anchorCache.set(file, anchors);
  return anchors;
}

function report(file, number, message) {
  problems.push(`${file.replace(/\\/g, "/")}:${number}: ${message}`);
}

function checkMarkdown(file) {
  for (const { line, number } of proseLines(fs.readFileSync(path.join(ROOT, file), "utf8"))) {
    const withoutInlineCode = line.replace(/`[^`]*`/g, "");
    for (const [, target] of withoutInlineCode.matchAll(/\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g)) {
      if (/^(https?:|mailto:)/.test(target)) continue;
      linksChecked++;
      const [rawPath, rawAnchor] = target.split("#");
      const targetFile = rawPath ? path.join(path.dirname(file), decodeURIComponent(rawPath)) : file;
      if (rawPath && !fs.existsSync(path.join(ROOT, targetFile))) {
        report(file, number, `link para "${rawPath}", que não existe`);
        continue;
      }
      if (rawAnchor !== undefined && targetFile.endsWith(".md")) {
        const anchor = decodeURIComponent(rawAnchor);
        if (!anchorsOf(targetFile).has(anchor)) {
          report(file, number, `âncora "#${anchor}" não existe em ${targetFile.replace(/\\/g, "/")}`);
        }
      }
    }
    checkSpecMentions(file, number, line);
  }
}

// "specs/<nome>.md" citado em texto (não só em link) precisa existir
function checkSpecMentions(file, number, line) {
  for (const [, name] of line.matchAll(/specs\/([\w-]+\.md)/g)) {
    if (name !== "README.md" && !fs.existsSync(path.join(ROOT, "docs", "specs", name))) {
      report(file, number, `cita a spec "${name}", que não existe em docs/specs/`);
    }
  }
}

// Só o texto de comentário da linha (código como "javax.crypto.spec" não conta)
function commentOf(line, extension) {
  const trimmed = line.trim();
  if (/^(\*|\/\*)/.test(trimmed)) return trimmed;
  if (extension === ".sql" && trimmed.includes("--")) return trimmed.slice(trimmed.indexOf("--"));
  if (extension === ".properties" && trimmed.startsWith("#")) return trimmed;
  if (extension === ".xml" && trimmed.includes("<!--")) return trimmed.slice(trimmed.indexOf("<!--"));
  const slashes = line.indexOf("//");
  return slashes >= 0 && line[slashes - 1] !== ":" ? line.slice(slashes) : "";
}

function checkCode(file) {
  const extension = path.extname(file);
  fs.readFileSync(path.join(ROOT, file), "utf8").split(/\r?\n/).forEach((line, index) => {
    if (/\bspecs?\b/i.test(commentOf(line, extension))) {
      report(file, index + 1, "comentário cita uma spec: cite um ADR ou uma seção de documento de referência");
    }
  });
}

const markdownFiles = [
  ...ROOT_DOCS.filter((file) => fs.existsSync(path.join(ROOT, file))),
  ...DOCS_DIRS.flatMap((dir) => walk(dir, (name) => name.endsWith(".md"))),
];
const codeFiles = CODE_DIRS.flatMap((dir) => walk(dir, (name) => CODE_EXTENSIONS.has(path.extname(name))));

markdownFiles.forEach(checkMarkdown);
codeFiles.forEach(checkCode);

if (problems.length > 0) {
  console.log(problems.join("\n"));
  console.log(`\n${problems.length} problema(s) encontrado(s).`);
  process.exit(1);
}
console.log(
  `OK: ${markdownFiles.length} documentos, ${linksChecked} links e ${codeFiles.length} arquivos de código conferidos.`
);
