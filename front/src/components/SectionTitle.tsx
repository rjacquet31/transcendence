import type { ReactNode } from 'react'
import s from './SectionTitle.module.css'

type SectionTitleProps = {
  /** Repris par l'`aria-labelledby` de la section englobante. */
  id?: string
  children: ReactNode
}

export function SectionTitle({ id, children }: SectionTitleProps) {
  return (
    <h2 id={id} className={s.sectionTitle}>
      {children}
    </h2>
  )
}
