import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { UNDERCOVER_MATCHES, type UndercoverMatchView } from '../lib/undercoverClient.ts'
import { useSocket } from './socketContext.ts'

/** Emmène sur la table les membres d'une salle dont l'hôte vient de lancer la
 *  partie, quelle que soit la page où ils se trouvent.
 *
 *  La même destination transporte chaque changement de la partie : seule une
 *  partie qui vient de démarrer — premier tour, aucun indice encore — mérite
 *  d'arracher l'utilisateur à la page où il est, et une seule fois. Sans ça,
 *  un premier joueur qui laisse filer son temps renverrait sur la table à
 *  chaque passage de tour quelqu'un qui en est volontairement parti. */
export function useUndercoverMatchStart() {
  const { subscribe } = useSocket()
  const navigate = useNavigate()
  const location = useLocation()
  const onUndercover = location.pathname === '/undercover'
  const openedRef = useRef(new Set<number>())

  useEffect(
    () =>
      subscribe(UNDERCOVER_MATCHES, (body) => {
        const match = JSON.parse(body) as UndercoverMatchView
        const justStarted =
          match.status === 'IN_PROGRESS' &&
          match.currentRound === 1 &&
          match.phase === 'DESCRIPTION' &&
          match.clues.length === 0
        if (!justStarted || openedRef.current.has(match.matchId)) return

        openedRef.current.add(match.matchId)
        if (!onUndercover) navigate('/undercover')
      }),
    [subscribe, navigate, onUndercover],
  )
}
