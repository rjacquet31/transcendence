/** Miroir du `ChessInvitationDto` du back (voir ChessPlayerChannelStomp). */
export type ChessInvitationDto = {
  invitationId: string
  fromUserId: string
  fromUsername: string
  state: ChessInvitationState
  /** Camp choisi par l'inviteur ; l'invité joue l'autre. */
  fromSide: ChessSide
}

/** `ACCEPTED` n'est jamais poussé ici : une acceptation arrive sous forme de match démarré. */
export type ChessInvitationState = 'PENDING' | 'DECLINED'

/** Une queue pour les deux sens : `PENDING` arrive chez l'invité, `DECLINED` chez l'inviteur. */
export const CHESS_INVITATIONS = '/user/queue/chess.invitations'

/** Démarrage et fin de partie passent tous deux par là, le `status` les sépare. */
export const CHESS_MATCHES = '/user/queue/chess.matches'

/** Le back ne pousse que le coup adverse : le sien est déjà connu par la réponse HTTP. */
export const CHESS_MOVES = '/user/queue/chess.moves'

export type ChessSide = 'WHITE' | 'BLACK'

export type ChessMatchStatus = 'IN_PROGRESS' | 'CHECKMATE' | 'STALEMATE' | 'DRAW' | 'RESIGNED'

/** `fen` seule fait foi (`sideToMove` en est relu) ; un camp bot a son `*PlayerId` à `null` et son `*BotLevel` renseigné. */
export type ChessMatchDto = {
  matchId: number
  fen: string
  sideToMove: ChessSide
  status: ChessMatchStatus
  winnerSide: ChessSide | null
  whitePlayerId: string
  whiteBotLevel: number | null
  blackPlayerId: string | null
  blackBotLevel: number | null
}

/** Le coup joué en UCI, et la partie telle qu'elle est *après* ce coup. */
export type ChessMovePlayedDto = {
  move: string
  match: ChessMatchDto
}
