import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { CHESS_MATCHES, type ChessMatchDto } from '../lib/chessClient.ts'
import { useSocket } from './socketContext.ts'

/** Emmène l'inviteur sur le plateau à l'acceptation (il ne l'apprend que par la socket) ; l'invité se redirige seul. */
export function useChessMatchStart() {
  const { subscribe } = useSocket()
  const navigate = useNavigate()

  useEffect(
    () =>
      subscribe(CHESS_MATCHES, (body) => {
        const match = JSON.parse(body) as ChessMatchDto
        // Même destination pour les fins de partie : seul un démarrage mérite de changer l'utilisateur de page.
        if (match.status === 'IN_PROGRESS') navigate('/chess')
      }),
    [subscribe, navigate],
  )
}
