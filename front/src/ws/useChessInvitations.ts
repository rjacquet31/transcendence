import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { acceptChessInvitation, declineChessInvitation, getPendingChessInvitations } from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import { CHESS_INVITATIONS, type ChessInvitationDto } from '../lib/chessClient.ts'
import type { Notification } from './notificationContext.ts'
import { useSocket } from './socketContext.ts'

/** Même durée que `ChessInvitation.TTL` du back ; le DTO n'a pas `createdAt`, une invitation rechargée repart pour 2 minutes. */
const INVITATION_TTL_MS = 2 * 60 * 1000

/** Pile des invitations d'échecs pour `<Toast>` et la cloche ; retirée par la croix, une réponse sans erreur ou 2 minutes. */
export function useChessInvitations() {
  const { subscribe, status } = useSocket()
  const { user } = useAuthModal()
  const navigate = useNavigate()
  const [invitations, setInvitations] = useState<Notification[]>([])
  const timers = useRef(new Map<string, number>())

  /** Changement de compte : rien de l'ancien ne doit rester affiché. */
  const [owner, setOwner] = useState(user?.id)
  if (owner !== user?.id) {
    setOwner(user?.id)
    setInvitations([])
  }

  const dismiss = useCallback((id: string) => {
    window.clearTimeout(timers.current.get(id))
    timers.current.delete(id)
    setInvitations((previous) => previous.filter((item) => item.id !== id))
  }, [])

  /** Ajoute une invitation (sans doublon) et arme son expiration. */
  const add = useCallback(
    (item: Notification) => {
      if (timers.current.has(item.id)) return
      timers.current.set(item.id, window.setTimeout(() => dismiss(item.id), INVITATION_TTL_MS))
      setInvitations((previous) => [...previous, item])
    },
    [dismiss],
  )

  /** Répond au back puis retire le toast ; en cas de refus il reste sans boutons, avec la raison. */
  const respond = useCallback(
    async (id: string, opponent: string, accept: boolean) => {
      try {
        if (accept) {
          await acceptChessInvitation(opponent)
          // La page Échecs retrouve la partie avec GET /chess/matches/me.
          navigate('/chess')
        } else {
          await declineChessInvitation(opponent)
        }
        dismiss(id)
      } catch (err: unknown) {
        setInvitations((previous) =>
          previous.map((item) =>
            item.id === id
              ? { ...item, body: errorMessage(err, 'Action impossible.'), actions: undefined }
              : item,
          ),
        )
      }
    },
    [navigate, dismiss],
  )

  /** `subscribe` renvoie sa désinscription, de référence stable : le provider replace la souscription à chaque reconnexion. */
  useEffect(
    () =>
      subscribe(CHESS_INVITATIONS, (body) => {
        const invitation = JSON.parse(body) as ChessInvitationDto
        add(toNotification(invitation, respond, true))
      }),
    [subscribe, respond, add],
  )

  /** À chaque (re)connexion : récupère ce qui est arrivé pendant qu'on n'écoutait pas, refresh compris. */
  useEffect(() => {
    if (status !== 'connected') return
    let cancelled = false
    getPendingChessInvitations()
      .then((pending) => {
        if (!cancelled) for (const invitation of pending) add(toNotification(invitation, respond, false))
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [status, add, respond])

  useEffect(() => {
    const pending = timers.current
    return () => {
      for (const timer of pending.values()) window.clearTimeout(timer)
      pending.clear()
    }
  }, [])

  return { invitations, dismiss }
}

/** Refus reçu = information sèche, invitation reçue = deux boutons. */
function toNotification(
  invitation: ChessInvitationDto,
  respond: (id: string, opponent: string, accept: boolean) => Promise<void>,
  popup: boolean,
): Notification {
  if (invitation.state === 'DECLINED') {
    return {
      id: invitation.invitationId,
      popup,
      title: 'Invitation refusée',
      body: `${invitation.fromUsername} a refusé ta partie d’échecs.`,
    }
  }

  return {
    id: invitation.invitationId,
    popup,
    title: 'Invitation aux échecs',
    body: `${invitation.fromUsername} t’invite à une partie d’échecs : tu joueras les ${
      invitation.fromSide === 'BLACK' ? 'blancs' : 'noirs'
    }.`,
    actions: [
      {
        label: 'Accepter',
        tone: 'accept',
        onClick: () => respond(invitation.invitationId, invitation.fromUsername, true),
      },
      {
        label: 'Refuser',
        tone: 'decline',
        onClick: () => respond(invitation.invitationId, invitation.fromUsername, false),
      },
    ],
  }
}
