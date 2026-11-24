// Tabela das listagens: cabeçalho no verde do menu e linhas listradas. Cada tela passa só as
// colunas e as linhas; o visual é todo daqui.
// columns: [{ key, header, cell: (row) => conteúdo, align: "left" | "right" | "center",
// emphasized: texto principal da linha (ex: o nome), em vez do tom secundário }]
import styles from "./DataTable.module.css";

function cellClass(column) {
  return [styles[column.align ?? "left"], column.emphasized && styles.emphasized].filter(Boolean).join(" ");
}

export function DataTable({ columns, rows, getRowKey = (row) => row.id, className }) {
  return (
    <div className={[styles.wrapper, className].filter(Boolean).join(" ")}>
      <table className={styles.table}>
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
            <tr key={getRowKey(row)}>
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
