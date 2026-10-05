import { glyphOf, isWhitePiece, nameOf } from '../lib/chessBoard.ts'
import s from './CapturedPieces.module.css'

type CapturedPiecesProps = {
  /** Lettres FEN des pièces prises, de la plus forte à la plus faible. */
  pieces: string[]
  /** Écart de matériel *de ce camp* : seul celui qui mène affiche le sien. */
  advantage: number
}

/** Les prises d'un camp en glyphes décoratifs ; la ligne entière porte le libellé. */
export function CapturedPieces({ pieces, advantage }: CapturedPiecesProps) {
  if (pieces.length === 0 && advantage <= 0) return null

  const label = pieces.length === 0 ? 'aucune prise' : pieces.map(nameOf).join(', ')

  return (
    <p className={s.captures} aria-label={`Prises : ${label}`}>
      {pieces.map((piece, index) => (
        <span
          key={`${piece}${index}`}
          className={isWhitePiece(piece) ? `${s.piece} ${s.white}` : s.piece}
          aria-hidden="true"
        >
          {glyphOf(piece)}
        </span>
      ))}

      {advantage > 0 && <span className={s.advantage}>+{advantage}</span>}
    </p>
  )
}
