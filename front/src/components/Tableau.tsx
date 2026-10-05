import type { ReactNode } from 'react'
import s from './Tableau.module.css'

/** Une colonne : un en-tête, et de quoi produire la cellule d'une ligne. */
export type Colonne<T> = {
  key: string
  header: string
  render: (row: T) => ReactNode
}

type TableauProps<T> = {
  /** Décrit le tableau aux lecteurs d'écran (rendu visuellement discret). */
  caption: string
  columns: Colonne<T>[]
  rows: T[]
  /** Clé React stable d'une ligne (un id, pas l'index). */
  rowKey: (row: T) => string
  emptyText: string
}

export function Tableau<T>({ caption, columns, rows, rowKey, emptyText }: TableauProps<T>) {
  return (
    <div className={s.scroller}>
      <table className={s.table}>
        <caption className="sr-only">{caption}</caption>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col" className={s.th}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td className={s.empty} colSpan={columns.length}>
                {emptyText}
              </td>
            </tr>
          ) : (
            rows.map((row) => (
              <tr key={rowKey(row)} className={s.tr}>
                {columns.map((column) => (
                  <td key={column.key} className={s.td}>
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
