import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { createBotMatch, getMyChessMatch, playChessMove, resignChessMatch } from '../lib/actions.ts'
import { ApiError, errorMessage } from '../lib/apiClient.ts'
import {
  CHESS_MATCHES,
  CHESS_MOVES,
  type ChessMatchDto,
  type ChessMovePlayedDto,
  type ChessSide,
} from '../lib/chessClient.ts'
import { useSocket } from './socketContext.ts'

/** Dernier coup reçu avec un compteur : deux fois le même coup restent deux événements distincts. */
type ServerMove = { move: string; count: number }

/** Détient la partie vue par le serveur (chargement initial, `chess.moves`, `chess.matches`) et rien d'autre ; la page se resynchronise sur `match.fen`. */
export function useChessMatch() {
  const { subscribe } = useSocket()
  const { user } = useAuthModal()
  const location = useLocation()

  const [match, setMatch] = useState<ChessMatchDto | null>(null)
  const [serverMove, setServerMove] = useState<ServerMove | null>(null)
  const [error, setError] = useState<string | null>(null)

  /* `location.key` et pas seulement le montage : accepter une invitation déjà sur /chess ne remonte pas le composant. */
  useEffect(() => {
    let alive = true

    getMyChessMatch()
      .then((current) => {
        if (alive) setMatch(current)
      })
      .catch((err: unknown) => {
        if (!alive) return
        // 404 = aucune partie (normal) ; 401/403 = session expirée, déjà gérée par la couche d'auth.
        const quiet = err instanceof ApiError && [404, 403, 401].includes(err.status)
        if (quiet) setMatch(null)
        else setError(errorMessage(err, 'Partie en cours introuvable.'))
      })

    return () => {
      alive = false
    }
  }, [location.key])

  useEffect(
    () =>
      subscribe(CHESS_MOVES, (body) => {
        const played = JSON.parse(body) as ChessMovePlayedDto
        setMatch(played.match)
        setServerMove((previous) => ({ move: played.move, count: (previous?.count ?? 0) + 1 }))
      }),
    [subscribe],
  )

  useEffect(
    () =>
      subscribe(CHESS_MATCHES, (body) => {
        // Démarrage comme fin de partie : le serveur envoie la partie entière.
        setMatch(JSON.parse(body) as ChessMatchDto)
      }),
    [subscribe],
  )

  /** Envoie un coup en UCI ; un refus laisse `match` sur la position serveur, ce qui annule le coup optimiste. */
  const play = async (matchId: number, move: string) => {
    setError(null)
    try {
      setMatch(await playChessMove(matchId, move))
    } catch (err: unknown) {
      setError(errorMessage(err, 'Coup refusé.'))
      // C'est la référence qui compte : elle relance la resynchronisation de la page sur le FEN serveur.
      setMatch((previous) => (previous === null ? null : { ...previous }))
    }
  }

  /** Ne rattrape pas l'erreur : l'appelant garde la modale ouverte et y affiche le refus. */
  const startBot = async (side: ChessSide, botLvl: number) => {
    setError(null)
    const created = await createBotMatch(side, botLvl)

    /* Rien n'arrive par la socket contre un bot : un coup resté d'une partie précédente serait rejoué sur celle-ci. */
    setServerMove(null)
    setMatch(created)
  }

  const resign = async (matchId: number) => {
    setError(null)
    try {
      setMatch(await resignChessMatch(matchId))
    } catch (err: unknown) {
      setError(errorMessage(err, 'Abandon impossible.'))
    }
  }

  const leave = () => {
    setMatch(null)
    setServerMove(null)
    setError(null)
  }

  const mySide: ChessSide | null =
    match === null || user === null ? null : match.whitePlayerId === user.id ? 'WHITE' : 'BLACK'

  const isMyTurn =
    match !== null && mySide !== null && match.status === 'IN_PROGRESS' && match.sideToMove === mySide

  return { match, mySide, isMyTurn, serverMove, error, play, resign, leave, startBot }
}
