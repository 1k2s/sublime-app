// Tabela das listagens, igual em todas as telas: cabeçalho claro e em negrito, parado no topo; linhas
// brancas, todas no mesmo cinza e sem negrito, com barra verde na linha com o mouse em cima. A
// rolagem é sempre da tabela, não da página. Cada tela passa só as colunas e as linhas; o visual é
// todo daqui.
// columns: [{ key, header, cell: (row) => conteúdo, align: "left" | "right" | "center" }]
import styles from "./DataTable.module.css";

// onRowClick(row): opcional; a linha inteira fica clicável (cursor de mão). Não substitui o botão
// de ação da linha, que é o caminho pelo teclado. selectedRowKey: a linha aberta (ex: no modal de
// detalhe) ou em destaque (ex: o último lançamento) fica marcada como no hover. compact: tabela com
// muitas colunas cabe na largura (títulos quebram linha, colunas mais juntas)
export function DataTable({
  columns,
  rows,
  getRowKey = (row) => row.id,
  onRowClick,
  selectedRowKey = null,
  compact = false,
  className,
}) {
  const tableClass = [styles.table, onRowClick && styles.clickable].filter(Boolean).join(" ");
  return (
    <div className={[styles.wrapper, compact && styles.compact, className].filter(Boolean).join(" ")}>
      <table className={tableClass}>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col" className={styles[column.align ?? "left"]}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={getRowKey(row)}
              className={selectedRowKey != null && getRowKey(row) === selectedRowKey ? styles.selected : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
            >
              {columns.map((column) => (
                <td key={column.key} className={styles[column.align ?? "left"]}>
                  {column.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
