import type { LucideIcon } from 'lucide-react'
import { Button } from './Button.tsx'
import s from './GameCard.module.css'

type GameCardProps = {
  name: string
  description: string
  icon: LucideIcon
  onPlay: () => void
}

export function GameCard({ name, description, icon: Icon, onPlay }: GameCardProps) {
  return (
    <article className={s.card}>
      <span className={s.icon} aria-hidden="true">
        <Icon size={56} strokeWidth={1.75} />
      </span>
      <h3 className={s.title}>{name}</h3>
      <p className={s.desc}>{description}</p>
      <div className={s.actions}>
        <Button variant="primary" fullWidth ariaLabel={`Jouer à ${name}`} onClick={onPlay}>
          Jouer
        </Button>
      </div>
    </article>
  )
}
