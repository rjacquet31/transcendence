import { useState } from 'react'
import { X } from 'lucide-react'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import type { ChessSide } from '../lib/chessClient.ts'
import s from './ChessBotModal.module.css'

/** Bornes imposées par le back (`@Min(1) @Max(10)` sur `ChessBotDto`). */
const LEVELS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

type ChessBotModalProps = {
  open: boolean
  /** Création en cours : verrouille le formulaire. */
  pending?: boolean
  /** Refus du back ; la modale reste ouverte pour que le message se lise. */
  error?: string | null
  onClose: () => void
  onStart: (side: ChessSide, botLvl: number) => void
}

/** Choix du camp et du niveau avant une partie contre le bot. */
export function ChessBotModal({
  open,
  pending = false,
  error,
  onClose,
  onStart,
}: ChessBotModalProps) {
  const dialog = useDialog(open, 'button', onClose)

  const [side, setSide] = useState<ChessSide>('WHITE')
  const [level, setLevel] = useState(3)

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="chess-bot-title">
      <div className={s.head}>
        <h2 id="chess-bot-title" className={s.title}>
          Jouer contre le bot
        </h2>
        <Button variant="ghost" ariaLabel="Fermer" onClick={onClose}>
          <X size={18} />
        </Button>
      </div>

      <div className={s.form}>
        <div className={s.field}>
          <span className={s.label} id="chess-bot-side">
            Votre camp
          </span>
          <div className={s.sides} role="group" aria-labelledby="chess-bot-side">
            <Button
              variant={side === 'WHITE' ? 'primary' : 'secondary'}
              fullWidth
              disabled={pending}
              onClick={() => setSide('WHITE')}
            >
              Blancs
            </Button>
            <Button
              variant={side === 'BLACK' ? 'primary' : 'secondary'}
              fullWidth
              disabled={pending}
              onClick={() => setSide('BLACK')}
            >
              Noirs
            </Button>
          </div>
        </div>

        <div className={s.field}>
          <label className={s.label} htmlFor="chess-bot-level">
            Niveau du bot
          </label>
          <select
            id="chess-bot-level"
            className={s.select}
            value={level}
            disabled={pending}
            onChange={(event) => setLevel(Number(event.target.value))}
          >
            {LEVELS.map((value) => (
              <option key={value} value={value}>
                Niveau {value}
              </option>
            ))}
          </select>
        </div>

        <p className={s.note}>
          {side === 'WHITE'
            ? 'Vous ouvrez la partie.'
            : 'Le bot ouvre : son premier coup sera déjà joué.'}
        </p>

        {error && (
          <p className={s.globalError} role="alert">
            {error}
          </p>
        )}

        <div className={s.actions}>
          <Button variant="secondary" disabled={pending} onClick={onClose}>
            Annuler
          </Button>
          <Button variant="primary" disabled={pending} onClick={() => onStart(side, level)}>
            {pending ? 'Lancement…' : 'Lancer la partie'}
          </Button>
        </div>
      </div>
    </dialog>
  )
}
