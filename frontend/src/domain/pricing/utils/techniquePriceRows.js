// Linhas da tabela de preços com a coluna Técnica: cada versão do plano (ver priceRows.js) se repete
// em cada técnica ativa do grupo. O preço é do grupo (ADR 0004): a mesma versão aparece com o mesmo
// valor em todas as técnicas dele, e reajustá-la muda o valor de todas.

// groups: os grupos da tela, na ordem da API. rowsByGroupId: Map do id do grupo para as linhas dele
// (buildPriceRows). techniques: as técnicas ativas (GET /api/techniques).
// Ordem: grupo, técnica por nome, e dentro da técnica a ordem das linhas do grupo. Grupo sem técnica
// ativa continua na tabela, com a técnica vazia (null), para o preço dele não sumir.
export function buildTechniquePriceRows({ groups, rowsByGroupId, techniques }) {
  return groups.flatMap((group) => {
    const groupRows = rowsByGroupId.get(group.id) ?? [];
    const groupTechniques = listGroupTechniques(techniques, group.id);
    const rowTechniques = groupTechniques.length > 0 ? groupTechniques : [null];
    return rowTechniques.flatMap((technique) =>
      groupRows.map((row) => ({
        ...row,
        key: `${group.id}-${technique?.id ?? "none"}-${row.key}`,
        rowKey: row.key,
        group,
        technique,
      })),
    );
  });
}

// Técnicas ativas do grupo, por nome
export function listGroupTechniques(techniques, pricingGroupId) {
  return techniques
    .filter((technique) => technique.pricingGroupId === pricingGroupId)
    .sort((a, b) => a.name.localeCompare(b.name, "pt-BR"));
}

// Filtros da tela: técnica ("ALL" ou o id) e status ("ALL", "CURRENT", "EXPIRED" ou "CLOSED")
export function filterTechniquePriceRows(rows, { techniqueId, status }) {
  return rows.filter(
    (row) => (techniqueId === "ALL" || row.technique?.id === techniqueId) && (status === "ALL" || row.status === status),
  );
}
