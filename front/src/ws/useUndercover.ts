import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  chooseUndercoverWords,
  configureUndercoverRoom,
  createUndercoverRoom,
  getMyUndercoverMatch,
  getMyUndercoverRoom,
  getMyUndercoverStats,
  joinUndercoverRoom,
  leaveUndercoverMatch,
  leaveUndercoverRoom,
  sendUndercoverChat,
  startUndercoverMatch,
  submitUndercoverClue,
  submitUndercoverGuess,
  submitUndercoverVote,
} from '../lib/actions.ts'
import { ApiError } from '../lib/apiClient.ts'
import {
  UNDERCOVER_CHAT,
  UNDERCOVER_MATCHES,
  UNDERCOVER_ROOMS,
  type UndercoverChatMessage,
  type UndercoverGameMode,
  type UndercoverMatchView,
  type UndercoverRoom,
  type UndercoverRules,
  type UndercoverStats,
  type UndercoverWordPair,
} from '../lib/undercoverClient.ts'
import { useSocket } from './socketContext.ts'

function messageOf(err: unknown, fallback: string) {
  return err instanceof ApiError ? err.message : fallback
}

/** 404 = rien en cours, cas normal. 401/403 = session expirée, dont la couche
 *  d'auth s'occupe déjà. */
function isQuiet(err: unknown) {
  return err instanceof ApiError && [404, 403, 401].includes(err.status)
}

/** Au-delà, les plus anciens messages du chat sont oubliés. */
const CHAT_LIMIT = 200

/** Détient ce que le serveur sait d'Undercover pour le compte connecté : sa
 *  salle, sa partie, le chat de cette partie et son bilan.
 *
 *  Trois sources pour un même état : le chargement initial (qui restaure tout
 *  après un rechargement de page), les réponses HTTP des actions, et les
 *  messages de la socket. Une vue de partie arrive entière à chaque fois : on
 *  garde simplement la plus récente, d'après sa `version`. */
export function useUndercover() {
  const { subscribe } = useSocket()
  const location = useLocation()

  const [room, setRoom] = useState<UndercoverRoom | null>(null)
  const [match, setMatch] = useState<UndercoverMatchView | null>(null)
  const [chat, setChat] = useState<UndercoverChatMessage[]>([])
  const [stats, setStats] = useState<UndercoverStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  /* Une partie qu'on a quittée ou refermée ne doit pas revenir par un message
     de la socket parti juste avant. */
  const dismissedRef = useRef(new Set<number>())

  /** Remplace la partie affichée, sauf par une vue plus ancienne de la même
   *  partie : deux messages proches peuvent se croiser. Une version égale est
   *  acceptée — un vote qui ne clôt pas le tour ne change pas la partie, mais
   *  change qui a déjà voté. */
  const acceptMatch = useCallback((next: UndercoverMatchView) => {
    if (dismissedRef.current.has(next.matchId)) return
    setMatch((current) =>
      current !== null && current.matchId === next.matchId && current.version > next.version
        ? current
        : next,
    )
  }, [])

  /** Relit la salle. Elle survit désormais à la partie : la fin d'une table y
   *  ramène ses joueurs, prêts à en relancer une. Un 404 veut dire qu'elle a
   *  été fermée entre-temps. */
  const refreshRoom = useCallback(async () => {
    try {
      setRoom(await getMyUndercoverRoom())
    } catch (err: unknown) {
      if (isQuiet(err)) setRoom(null)
    }
  }, [])

  const refreshStats = useCallback(() => {
    getMyUndercoverStats()
      .then(setStats)
      .catch(() => {
        /* Le bilan est accessoire : la page reste utilisable sans. */
      })
  }, [])

  /* `location.key` et pas seulement le montage : revenir sur la page par la
     barre latérale doit relire l'état, même sans remonter le composant. */
  useEffect(() => {
    let alive = true
    setLoading(true)

    Promise.allSettled([getMyUndercoverMatch(), getMyUndercoverRoom(), getMyUndercoverStats()])
      .then(([matchResult, roomResult, statsResult]) => {
        if (!alive) return

        if (matchResult.status === 'fulfilled') acceptMatch(matchResult.value)
        else if (isQuiet(matchResult.reason)) setMatch(null)
        else setError(messageOf(matchResult.reason, 'Partie en cours introuvable.'))

        /* Gardée même pendant la partie, sinon un rechargement de page en
           cours de table perdrait la salle où l'écran de fin doit ramener. */
        if (roomResult.status === 'fulfilled') {
          setRoom(roomResult.value)
        } else if (!isQuiet(roomResult.reason)) {
          setError(messageOf(roomResult.reason, 'Salle introuvable.'))
        }

        if (statsResult.status === 'fulfilled') setStats(statsResult.value)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [location.key, acceptMatch])

  useEffect(
    () =>
      subscribe(UNDERCOVER_ROOMS, (body) => {
        setRoom(JSON.parse(body) as UndercoverRoom)
      }),
    [subscribe],
  )

  useEffect(
    () =>
      subscribe(UNDERCOVER_MATCHES, (body) => {
        const next = JSON.parse(body) as UndercoverMatchView
        acceptMatch(next)
        if (next.status !== 'IN_PROGRESS') refreshStats()
      }),
    [subscribe, acceptMatch, refreshStats],
  )

  useEffect(
    () =>
      subscribe(UNDERCOVER_CHAT, (body) => {
        const message = JSON.parse(body) as UndercoverChatMessage
        setChat((previous) => [...previous, message].slice(-CHAT_LIMIT))
      }),
    [subscribe],
  )

  /** Enveloppe commune : une action à la fois, et un refus du back devient un
   *  message affiché plutôt qu'une exception. */
  const run = useCallback(async <T,>(action: () => Promise<T>, fallback: string) => {
    setPending(true)
    setError(null)
    try {
      return await action()
    } catch (err: unknown) {
      setError(messageOf(err, fallback))
      return undefined
    } finally {
      setPending(false)
    }
  }, [])

  const createRoom = useCallback(async () => {
    const created = await run(createUndercoverRoom, 'Création de la salle impossible.')
    if (created) setRoom(created)
  }, [run])

  const joinRoom = useCallback(
    async (code: string) => {
      const joined = await run(() => joinUndercoverRoom(code), 'Impossible de rejoindre la salle.')
      if (joined) setRoom(joined)
      return joined !== undefined
    },
    [run],
  )

  const leaveRoom = useCallback(async () => {
    await run(leaveUndercoverRoom, 'Impossible de quitter la salle.')
    setRoom(null)
  }, [run])

  const configure = useCallback(
    async (mode: UndercoverGameMode, rules: UndercoverRules | null) => {
      const updated = await run(
        () => configureUndercoverRoom(mode, rules),
        'Réglages refusés.',
      )
      if (updated) setRoom(updated)
      return updated !== undefined
    },
    [run],
  )

  const chooseWords = useCallback(
    async (categories: string[], wordPairs: UndercoverWordPair[]) => {
      const updated = await run(() => chooseUndercoverWords(categories, wordPairs), 'Mots refusés.')
      if (updated) setRoom(updated)
      return updated !== undefined
    },
    [run],
  )

  const start = useCallback(async () => {
    const started = await run(startUndercoverMatch, 'Lancement impossible.')
    if (started) {
      setChat([])
      acceptMatch(started)
    }
  }, [run, acceptMatch])

  const clue = useCallback(
    async (word: string) => {
      if (match === null) return false
      const next = await run(() => submitUndercoverClue(match.matchId, word), 'Indice refusé.')
      if (next) acceptMatch(next)
      return next !== undefined
    },
    [run, match, acceptMatch],
  )

  const vote = useCallback(
    async (targetSeat: number) => {
      if (match === null) return
      const next = await run(() => submitUndercoverVote(match.matchId, targetSeat), 'Vote refusé.')
      if (next) acceptMatch(next)
    },
    [run, match, acceptMatch],
  )

  const guess = useCallback(
    async (word: string) => {
      if (match === null) return false
      const next = await run(() => submitUndercoverGuess(match.matchId, word), 'Devinette refusée.')
      if (next) {
        acceptMatch(next)
        if (next.status !== 'IN_PROGRESS') refreshStats()
      }
      return next !== undefined
    },
    [run, match, acceptMatch, refreshStats],
  )

  const sendChat = useCallback(
    async (text: string) => {
      if (match === null) return false
      const sent = await run(async () => {
        await sendUndercoverChat(match.matchId, text)
        return true
      }, 'Message refusé.')
      return sent === true
    },
    [run, match],
  )

  /** Quitte la table : le serveur élimine le joueur s'il était encore en jeu.
   *  On revient dans la salle, qui a survécu à la partie. */
  const leaveMatch = useCallback(async () => {
    if (match === null) return false
    const matchId = match.matchId
    const left = await run(async () => {
      await leaveUndercoverMatch(matchId)
      return true
    }, 'Impossible de quitter la partie.')
    if (left) {
      dismissedRef.current.add(matchId)
      setMatch(null)
      setChat([])
      refreshStats()
      void refreshRoom()
    }
    return left === true
  }, [run, match, refreshStats, refreshRoom])

  /** Referme l'écran de fin : la partie est terminée côté serveur, il n'y a
   *  plus rien à lui dire. La salle est relue pour retrouver ses joueurs. */
  const closeFinished = useCallback(() => {
    if (match !== null) dismissedRef.current.add(match.matchId)
    setMatch(null)
    setChat([])
    setError(null)
    void refreshRoom()
  }, [match, refreshRoom])

  const matchChat = match === null ? [] : chat.filter((message) => message.matchId === match.matchId)

  return {
    room,
    match,
    chat: matchChat,
    stats,
    loading,
    pending,
    error,
    clearError: () => setError(null),
    createRoom,
    joinRoom,
    leaveRoom,
    configure,
    chooseWords,
    start,
    clue,
    vote,
    guess,
    sendChat,
    leaveMatch,
    closeFinished,
  }
}
