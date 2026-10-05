import { useState, type ReactNode } from 'react'
import { Toast } from '../components/Toast.tsx'
import { NotificationContext, type Notification } from './notificationContext.ts'
import { CHAT_TOAST_PREFIX, useChatNotifications } from './useChatNotifications.ts'
import { useChessInvitations } from './useChessInvitations.ts'
import { FRIEND_TOAST_PREFIX, useFriendNotifications } from './useFriendNotifications.ts'

/** Source unique des notifications : une pile de toasts et, via le contexte, la cloche ; chaque hook préfixe ses ids (UUID nu pour les invitations d'échecs). */
export function NotificationCenter({ children }: { children: ReactNode }) {
  const chess = useChessInvitations()
  const friends = useFriendNotifications()
  const chat = useChatNotifications()

  /** La croix masque le toast, la notification reste dans la cloche ; on retient l'objet, pas l'id, pour qu'une notification réécrite ressorte. */
  const [hidden, setHidden] = useState<ReadonlySet<Notification>>(new Set())

  const items = [...chess.invitations, ...friends.items, ...chat.items]

  const dismiss = (id: string) => {
    if (id.startsWith(FRIEND_TOAST_PREFIX)) friends.dismiss(id)
    else if (id.startsWith(CHAT_TOAST_PREFIX)) chat.dismiss(id)
    else chess.dismiss(id)
  }

  const hide = (id: string) => {
    const item = items.find((candidate) => candidate.id === id)
    if (item) setHidden((previous) => new Set(previous).add(item))
  }

  return (
    <NotificationContext.Provider value={{ items, dismiss }}>
      <Toast items={items.filter((item) => item.popup && !hidden.has(item))} onDismiss={hide} />
      {children}
    </NotificationContext.Provider>
  )
}
