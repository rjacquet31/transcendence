/** Traduit une partie chess.js en ce que la vue dessine : 64 cases, glyphes, libellés ; aucune règle ici. */

import type { Chess } from 'chess.js'

/** Position de départ, telle que la renvoie `new Board()` côté back. */
export const START_FEN = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1'

const FILES = ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']
const RANKS = ['8', '7', '6', '5', '4', '3', '2', '1']

/** `['a8', 'b8', …, 'h1']` — même ordre que les cases renvoyées par `squaresOf`. */
export const SQUARE_NAMES = RANKS.flatMap((rank) => FILES.map((file) => file + rank))

/** Glyphes pleins pour les deux couleurs (les creux disparaissent sur cases claires) ; la couleur vient du CSS. */
const GLYPHS: Record<string, string> = {
  k: '♚',
  q: '♛',
  r: '♜',
  b: '♝',
  n: '♞',
  p: '♟',
}

const NAMES: Record<string, string> = {
  k: 'roi',
  q: 'dame',
  r: 'tour',
  b: 'fou',
  n: 'cavalier',
  p: 'pion',
}

/** Pièces dont le nom est féminin : leur couleur s'accorde (« tour noire »). */
const FEMININE = new Set(['q', 'r'])

export function glyphOf(piece: string) {
  return GLYPHS[piece.toLowerCase()] ?? ''
}

/** `null` pour une case vide, sinon la lettre FEN (majuscule = blancs). */
export function isWhitePiece(piece: string) {
  return piece === piece.toUpperCase()
}

/** Libellé lisible pour les lecteurs d'écran : « pion blanc », « tour noire ». */
export function nameOf(piece: string) {
  const type = piece.toLowerCase()
  const white = isWhitePiece(piece)

  let color: string
  if (FEMININE.has(type)) color = white ? 'blanche' : 'noire'
  else color = white ? 'blanc' : 'noir'

  return `${NAMES[type] ?? 'pièce'} ${color}`
}

/** Aplatit `chess.board()` en 64 lettres FEN, index 0 = a8 à 63 = h1, dans l'ordre de `SQUARE_NAMES`. */
export function squaresOf(chess: Chess): (string | null)[] {
  return chess
    .board()
    .flat()
    .map((cell) => {
      if (cell === null) return null
      return cell.color === 'w' ? cell.type.toUpperCase() : cell.type
    })
}

/** Matériel d'un camp au départ et valeur de chaque pièce ; le roi ne se prend ni ne se compte. */
const INITIAL: Record<string, number> = { q: 1, r: 2, b: 2, n: 2, p: 8 }
const VALUES: Record<string, number> = { q: 9, r: 5, b: 3, n: 3, p: 1 }

export type Captures = {
  /** Pièces noires disparues — les prises des blancs. */
  byWhite: string[]
  /** Pièces blanches disparues — les prises des noirs. */
  byBlack: string[]
  /** Positif si les blancs mènent, négatif si ce sont les noirs. */
  advantage: number
}

/** Compte les pièces d'un camp encore sur l'échiquier, par type. */
function materialOf(squares: (string | null)[], white: boolean) {
  const counts: Record<string, number> = { q: 0, r: 0, b: 0, n: 0, p: 0 }

  for (const piece of squares) {
    if (piece === null || isWhitePiece(piece) !== white) continue
    const type = piece.toLowerCase()
    if (type in counts) counts[type] += 1
  }

  return counts
}

/** Pièces d'un camp absentes de l'échiquier, de la plus forte à la plus faible ; une promotion rend une pièce en surnombre. */
function missingOf(counts: Record<string, number>, white: boolean) {
  let promoted = 0
  for (const type of ['q', 'r', 'b', 'n']) {
    promoted += Math.max(0, counts[type] - INITIAL[type])
  }

  const missing: string[] = []
  for (const type of ['q', 'r', 'b', 'n', 'p']) {
    const gone = Math.max(0, INITIAL[type] - counts[type]) - (type === 'p' ? promoted : 0)
    for (let i = 0; i < gone; i += 1) missing.push(white ? type.toUpperCase() : type)
  }

  return missing
}

function scoreOf(counts: Record<string, number>) {
  return Object.entries(counts).reduce((total, [type, count]) => total + VALUES[type] * count, 0)
}

/** Déduit les prises de la seule position : une partie reprise depuis un FEN arrive sans coups. */
export function capturesOf(squares: (string | null)[]): Captures {
  const white = materialOf(squares, true)
  const black = materialOf(squares, false)

  return {
    byWhite: missingOf(black, false),
    byBlack: missingOf(white, true),
    advantage: scoreOf(white) - scoreOf(black),
  }
}
