/** Destinations STOMP d'Undercover, toutes préfixées `/user` : le broker ne
 *  livre qu'à la session du destinataire. C'est indispensable pour les parties,
 *  dont chaque vue contient le mot de son destinataire. */

/** La salle telle qu'elle est après chaque arrivée, départ ou réglage. */
export const UNDERCOVER_ROOMS = '/user/queue/undercover.rooms'

/** La partie, vue par le destinataire, après chaque changement : démarrage,
 *  indice, vote, élimination, chrono écoulé, fin. */
export const UNDERCOVER_MATCHES = '/user/queue/undercover.matches'

/** Le chat de la partie. Rien n'est enregistré : un message raté est perdu. */
export const UNDERCOVER_CHAT = '/user/queue/undercover.chat'

export type UndercoverGameMode = 'CLASSIC' | 'BATTLEROYAL' | 'CUSTOM'

export type UndercoverPhase = 'DESCRIPTION' | 'VOTE' | 'MR_WHITE_GUESS' | 'END'

export type UndercoverMatchStatus = 'IN_PROGRESS' | 'AGENTS_WIN' | 'UNDERCOVER_WIN' | 'MR_WHITE_WIN'

export type UndercoverRole = 'AGENT' | 'UNDERCOVER' | 'MR_WHITE'

/** Miroir de `UndercoverRulesDto`. `maxVotes` à `null` : autant de votes que
 *  nécessaire. Les durées sont en secondes. */
export type UndercoverRules = {
  undercoverCount: number
  mrWhiteCount: number
  descriptionRounds: number
  maxVotes: number | null
  descriptionSeconds: number
  voteSeconds: number
  mrWhiteVoteSeconds: number
  undercoverWinOnParity: boolean
  tieRevotes: number
  randomOnUnresolvedTie: boolean
}

/** `userName` à `null` si le compte a été supprimé entre-temps. */
export type UndercoverUser = {
  userId: string
  userName: string | null
  avatarUrl: string | null
}

/** Deux mots proches : celui des agents et celui de l'undercover. */
export type UndercoverWordPair = {
  agentsWord: string
  undercoverWord: string
}

/** Bornes de la liste de mots perso, vérifiées aussi par le back. */
export const MAX_CUSTOM_PAIRS = 50
export const MAX_WORD_LENGTH = 40

/** Miroir de `UndercoverRoomDto`. `rules` : les règles avec lesquelles la
 *  partie serait jouée si elle démarrait maintenant. */
export type UndercoverRoom = {
  code: string
  hostId: string
  mode: UndercoverGameMode
  rules: UndercoverRules
  members: UndercoverUser[]
  minPlayers: number
  maxPlayers: number
  /** Catégories dans lesquelles les mots sont tirés ; vide pour toutes. Mises
   *  de côté tant que la liste perso sert. */
  categories: string[]
  /** Nombre de paires de la liste perso ; 0 quand on joue sur les mots du jeu. */
  customWordCount: number
  /** La liste perso elle-même, envoyée à l'hôte seul : `null` pour les autres,
   *  qui liraient sinon les mots avant la partie. */
  customWordPairs: UndercoverWordPair[] | null
}

/** Un siège de la table. `role` reste `null` tant qu'il est secret : il est
 *  révélé à l'élimination du joueur, et pour tout le monde en fin de partie. */
export type UndercoverPlayer = {
  seat: number
  userId: string
  userName: string | null
  avatarUrl: string | null
  eliminated: boolean
  eliminatedRound: number | null
  leftMatch: boolean
  role: UndercoverRole | null
}

export type UndercoverClue = {
  round: number
  seat: number
  word: string
}

/** Miroir de `UndercoverMatchViewDto` : la partie telle que **ce** joueur a le
 *  droit de la voir. Règle officielle : on reçoit son mot, pas son rôle — un
 *  undercover ne sait pas qu'il l'est — sauf Mr White, qui n'a pas de mot.
 *
 *  Les joueurs sont désignés par leur siège, jamais par un identifiant interne. */
export type UndercoverMatchView = {
  matchId: number
  /** Croît à chaque changement : les messages de la socket peuvent arriver
   *  dans le désordre, on garde la vue la plus récente. */
  version: number
  mode: UndercoverGameMode
  rules: UndercoverRules
  status: UndercoverMatchStatus
  phase: UndercoverPhase
  currentRound: number
  currentSpeakerSeat: number | null
  /** Date ISO de fin de la phase, `null` une fois la partie finie. */
  phaseDeadline: string | null
  /** Heure du serveur à l'envoi : corrige le compte à rebours d'une horloge
   *  locale qui dérive. */
  serverTime: string
  votesUsed: number
  voteAttempt: number
  /** Qui a déjà voté dans ce tour de vote — jamais contre qui. */
  votedSeats: number[]
  /** Pendant un revote : les seuls sièges contre lesquels on peut voter. */
  tiedSeats: number[]
  /** Pendant la devinette : le siège du Mr White qui doit répondre. */
  guesserSeat: number | null
  mySeat: number
  /** `null` pour Mr White. */
  myWord: string | null
  iAmMrWhite: boolean
  iAmEliminated: boolean
  players: UndercoverPlayer[]
  clues: UndercoverClue[]
  /** Les deux mots, une fois la partie finie seulement. */
  agentsWord: string | null
  undercoverWord: string | null
}

export type UndercoverChatMessage = {
  matchId: number
  seat: number
  userName: string | null
  text: string
  sentAt: string
}

export type UndercoverStats = {
  wins: number
  losses: number
}
