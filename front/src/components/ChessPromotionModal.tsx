import { X } from 'lucide-react'
import { glyphOf, nameOf } from '../lib/chessBoard.ts'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import s from './ChessPromotionModal.module.css'

/** Les quatre promotions autorisées, de la plus forte à la plus faible. */
export type PromotionPiece = 'q' | 'r' | 'b' | 'n'

const CHOICES: PromotionPiece[] = ['q', 'r', 'b', 'n']

type ChessPromotionModalProps = {
  open: boolean
  /** Camp du pion promu : décide de la couleur des glyphes proposés. */
  white: boolean
  /** Renonce à la promotion : le coup n'est pas joué, le pion ne bouge pas. */
  onClose: () => void
  onSelect: (piece: PromotionPiece) => void
}

/** Choix de la pièce de promotion ; refermer annule le coup et le pion reste en place. */
export function ChessPromotionModal({
  open,
  white,
  onClose,
  onSelect,
}: ChessPromotionModalProps) {
  const dialog = useDialog(open, 'button', onClose)

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="chess-promotion-title">
      <div className={s.head}>
        <h2 id="chess-promotion-title" className={s.title}>
          Promotion
        </h2>
        <Button variant="ghost" ariaLabel="Annuler le coup" onClick={onClose}>
          <X size={18} />
        </Button>
      </div>

      <p className={s.note}>
        Votre pion arrive au bout : choisissez sa pièce, ou refermez pour annuler le coup.
      </p>

      <div className={s.choices} role="group" aria-labelledby="chess-promotion-title">
        {CHOICES.map((piece) => {
          const label = nameOf(white ? piece.toUpperCase() : piece)

          return (
            <button
              key={piece}
              type="button"
              className={s.choice}
              aria-label={label}
              onClick={() => onSelect(piece)}
            >
              <span className={white ? `${s.piece} ${s.white}` : s.piece} aria-hidden="true">
                {glyphOf(piece)}
              </span>
              <span className={s.name}>{label.split(' ')[0]}</span>
            </button>
          )
        })}
      </div>
    </dialog>
  )
}
