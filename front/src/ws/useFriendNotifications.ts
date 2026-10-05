import { useCallback, useEffect, useState } from 'react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { useChat } from '../chat/chatContext.ts'
import { acceptFriendRequest, declineFriendRequest, getFriendList } from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import { FRIEND_NOTIFICATIONS, type FriendNotificationDto } from '../lib/notificationClient.ts'
import type { Notification } from './notificationContext.ts'
import { useSocket } from './socketContext.ts'

/** Préfixe des ids de toast, pour que `NotificationCenter` sache à quel hook rendre la fermeture. */
export const FRIEND_TOAST_PREFIX = 'friend:'

/** Pile des notifications d'amis, sur le modèle de `useChessInvitations` ; relance la liste d'amis, donc sous `ChatProvider`. */
export function useFriendNotifications() {
  const { subscribe, status } = useSocket()
  const { refresh } = useChat()
  const { user } = useAuthModal()
  const [items, setItems] = useState<Notification[]>([])

  /** Changement de compte : rien de l'ancien ne doit rester affiché. */
  const [owner, setOwner] = useState(user?.id)
  if (owner !== user?.id) {
    setOwner(user?.id)
    setItems([])
  }

  const dismiss = (id: string) => {
    setItems((previous) => previous.filter((item) => item.id !== id))
  }

  /** Succès = toast retiré ; échec = toast gardé sans boutons, avec la raison. */
  const respond = useCallback(
    async (id: string, idUser: string, accept: boolean) => {
      try {
        if (accept) await acceptFriendRequest(idUser)
        else await declineFriendRequest(idUser)
        setItems((previous) => previous.filter((item) => item.id !== id))
        void refresh().catch(() => {})
      } catch (err: unknown) {
        setItems((previous) =>
          previous.map((item) =>
            item.id === id
              ? { ...item, body: errorMessage(err, 'Action impossible.'), actions: undefined }
              : item,
          ),
        )
      }
    },
    [refresh],
  )

  useEffect(
    () =>
      subscribe(FRIEND_NOTIFICATIONS, (body) => {
        const notification = JSON.parse(body) as FriendNotificationDto
        const toast = toNotification(notification, respond, true)
        // Une nouvelle demande du même compte remplace l'ancienne.
        setItems((previous) => [...previous.filter((item) => item.id !== toast.id), toast])
        void refresh().catch(() => {})
      }),
    [subscribe, respond, refresh],
  )

  /** À chaque (re)connexion : les demandes en attente sont les lignes PENDING dont l'autre est l'expéditeur. */
  useEffect(() => {
    if (status !== 'connected') return
    let cancelled = false
    getFriendList()
      .then((rows) => {
        if (cancelled) return
        const received = rows
          .filter((row) => row.status === 'PENDING' && row.senderId === row.idUser)
          .map((row) =>
            toNotification(
              { type: 'REQUEST_RECEIVED', fromUserId: row.idUser, fromUserName: row.userName },
              respond,
              false,
            ),
          )
        setItems((previous) => [
          ...previous,
          ...received.filter((item) => !previous.some((known) => known.id === item.id)),
        ])
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [status, respond])

  return { items, dismiss }
}

function toNotification(
  notification: FriendNotificationDto,
  respond: (id: string, idUser: string, accept: boolean) => Promise<void>,
  popup: boolean,
): Notification {
  const id = `${FRIEND_TOAST_PREFIX}${notification.type}:${notification.fromUserId}`

  if (notification.type === 'REQUEST_ACCEPTED') {
    return {
      id,
      popup,
      title: 'Demande acceptée',
      body: `${notification.fromUserName} a accepté ta demande d’ami.`,
    }
  }

  return {
    id,
    popup,
    title: 'Demande d’ami',
    body: `${notification.fromUserName} veut t’ajouter en ami.`,
    actions: [
      {
        label: 'Accepter',
        tone: 'accept',
        onClick: () => respond(id, notification.fromUserId, true),
      },
      {
        label: 'Refuser',
        tone: 'decline',
        onClick: () => respond(id, notification.fromUserId, false),
      },
    ],
  }
}
