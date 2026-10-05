import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { useChat } from '../chat/chatContext.ts'
import { CHAT_NOTIFICATIONS, type ChatNotificationDto } from '../lib/notificationClient.ts'
import type { Notification } from './notificationContext.ts'
import { useSocket } from './socketContext.ts'

export const CHAT_TOAST_PREFIX = 'chat:'

/** Le toast affiché pour une conversation, et le nombre de messages qu'il résume. */
type ChatToast = { item: Notification; count: number }

/** Un seul toast par expéditeur, réécrit à chaque message. Rien ne s'affiche si
 *  la conversation est déjà sous les yeux (page `/chat/:idUser` ou quick chat),
 *  et le toast disparaît dès qu'on l'ouvre. Doit vivre sous `ChatProvider`. */
export function useChatNotifications() {
  const { subscribe } = useSocket()
  const { quickChatIds, openQuickChat } = useChat()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const { user } = useAuthModal()
  const [toasts, setToasts] = useState<Record<string, ChatToast>>({})

  /** Changement de compte : rien de l'ancien ne doit rester affiché. */
  const [owner, setOwner] = useState(user?.id)
  if (owner !== user?.id) {
    setOwner(user?.id)
    setToasts({})
  }

  /** Lus depuis le handler STOMP via des refs, pour ne pas réinscrire la file
   *  à chaque navigation. */
  const pathnameRef = useRef(pathname)
  const quickChatIdsRef = useRef(quickChatIds)

  const dismiss = useCallback((id: string) => {
    const senderId = id.slice(CHAT_TOAST_PREFIX.length)
    setToasts((previous) => {
      if (!(senderId in previous)) return previous
      const next = { ...previous }
      delete next[senderId]
      return next
    })
  }, [])

  /** Retire le toast d'une conversation qu'on vient d'ouvrir. Fait pendant le
   *  rendu plutôt que dans un effet : sinon le toast clignoterait. */
  const [seen, setSeen] = useState({ pathname, quickChatIds })
  if (seen.pathname !== pathname || seen.quickChatIds !== quickChatIds) {
    setSeen({ pathname, quickChatIds })
    const open = Object.keys(toasts).filter((senderId) => isOpen(senderId, pathname, quickChatIds))
    if (open.length > 0) {
      const next = { ...toasts }
      for (const senderId of open) delete next[senderId]
      setToasts(next)
    }
  }

  useEffect(() => {
    pathnameRef.current = pathname
    quickChatIdsRef.current = quickChatIds
  }, [pathname, quickChatIds])

  useEffect(
    () =>
      subscribe(CHAT_NOTIFICATIONS, (body) => {
        const notification = JSON.parse(body) as ChatNotificationDto
        const { senderId } = notification
        if (isOpen(senderId, pathnameRef.current, quickChatIdsRef.current)) return

        const id = `${CHAT_TOAST_PREFIX}${senderId}`
        setToasts((previous) => {
          const count = (previous[senderId]?.count ?? 0) + 1
          const item: Notification = {
            id,
            popup: true,
            title: notification.senderUserName,
            body: count > 1 ? `${count} nouveaux messages — ${notification.preview}` : notification.preview,
            actions: [
              { label: 'Répondre', onClick: () => openQuickChat(senderId) },
              { label: 'Ouvrir', onClick: () => navigate(`/chat/${senderId}`) },
            ],
          }
          return { ...previous, [senderId]: { item, count } }
        })
      }),
    [subscribe, openQuickChat, navigate],
  )

  // Les deux actions ouvrent la conversation : le toast part au rendu suivant.
  const items = Object.values(toasts).map((toast) => toast.item)

  return { items, dismiss }
}

function isOpen(senderId: string, pathname: string, quickChatIds: string[]) {
  return pathname === `/chat/${senderId}` || quickChatIds.includes(senderId)
}
