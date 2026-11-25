// Tabela das listagens: cabeçalho no verde do menu e linhas listradas. variant="light": cabeçalho
// claro e linhas brancas, com barra verde na linha com o mouse em cima (ex: Contratos). Cada
// tela passa só as
// colunas e as linhas; o visual é todo daqui.
// columns: [{ key, header, cell: (row) => conteúdo, align: "left" | "right" | "center",
// emphasized: texto principal da linha (ex: o nome), em vez do tom secundário }]
import styles from "./DataTable.module.css";

function cellClass(column) {
  return [styles[column.align ?? "left"], column.emphasized && styles.emphasized].filter(Boolean).join(" ");
}

// onRowClick(row): opcional; a linha inteira fica clicável (cursor de mão). Não substitui o botão
// de ação da linha, que é o caminho pelo teclado. selectedRowKey: a linha aberta (ex: no modal de
// detalhe) fica marcada como no hover (na variante clara)
export function DataTable({
  columns,
  rows,
  getRowKey = (row) => row.id,
  variant = "default",
  onRowClick,
  selectedRowKey = null,
  className,
}) {
  const tableClass = [styles.table, variant === "light" && styles.light, onRowClick && styles.clickable]
    .filter(Boolean)
    .join(" ");
  return (
    <div className={[styles.wrapper, className].filter(Boolean).join(" ")}>
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
                <td key={column.key} className={cellClass(column)}>
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
