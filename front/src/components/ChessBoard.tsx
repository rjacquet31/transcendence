import { SQUARE_NAMES, glyphOf, isWhitePiece, nameOf } from '../lib/chessBoard.ts'
import s from './ChessBoard.module.css'

export type BoardOrientation = 'white' | 'black'

type ChessBoardProps = {
  /** 64 cases, index 0 = a8 : lettre FEN de la pièce, ou `null` si vide. */
  squares: (string | null)[]
  /** Camp affiché en bas de l'échiquier. */
  orientation: BoardOrientation
  /** Case en attente de destination, mise en avant. */
  selected: string | null
  /** Cases d'arrivée légales pour la pièce sélectionnée. */
  targets: string[]
  /** Départ et arrivée du dernier coup joué, teintés pour le retrouver. */
  lastMove: { from: string; to: string } | null
  onSquareClick: (square: string) => void
}

/** Vue pure : dessine la position reçue et remonte les clics ; chaque case est un bouton, jouable au clavier. */
export function ChessBoard({
  squares,
  orientation,
  selected,
  targets,
  lastMove,
  onSquareClick,
}: ChessBoardProps) {
  /* Vue noirs : on lit le plateau à l'envers, cases *et* coordonnées suivent. */
  const order = SQUARE_NAMES.map((square, index) => ({ square, index }))
  if (orientation === 'black') order.reverse()

  return (
    <div className={s.board} role="group" aria-label="Échiquier">
      {order.map(({ square, index }, position) => {
        const piece = squares[index]
        const isLight = (Math.floor(index / 8) + (index % 8)) % 2 === 0
        const isLastMove = lastMove !== null && (lastMove.from === square || lastMove.to === square)
        const isTarget = targets.includes(square)

        const classes = [s.square, isLight ? s.light : s.dark]
        if (square === selected) classes.push(s.selected)
        if (isLastMove) classes.push(s.lastMove)
        /* Pastille sur case vide, anneau sur pièce à prendre : la pièce reste visible. */
        if (isTarget) classes.push(piece === null ? s.target : s.capture)

        const pieceLabel = piece === null ? 'case vide' : nameOf(piece)
        const targetLabel = isTarget ? (piece === null ? ', coup possible' : ', prise possible') : ''

        return (
          <button
            key={square}
            type="button"
            className={classes.join(' ')}
            aria-pressed={square === selected}
            aria-label={`${square}, ${pieceLabel}${targetLabel}`}
            onClick={() => onSquareClick(square)}
          >
            {piece === null ? null : (
              <span
                className={isWhitePiece(piece) ? `${s.piece} ${s.white}` : s.piece}
                aria-hidden="true"
              >
                {glyphOf(piece)}
              </span>
            )}

            {/* Coordonnées dans les marges : rangées à gauche, colonnes en bas. */}
            {position % 8 === 0 ? (
              <span className={s.rankLabel} aria-hidden="true">
                {square[1]}
              </span>
            ) : null}
            {position >= 56 ? (
              <span className={s.fileLabel} aria-hidden="true">
                {square[0]}
              </span>
            ) : null}
          </button>
        )
      })}
    </div>
  )
}
