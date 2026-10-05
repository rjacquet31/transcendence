import { createContext, useContext } from 'react'
import type { ToastItem } from '../components/Toast.tsx'

/** Entrée de la boîte de réception ; `popup: false` pour ce qui est rechargé du back (cloche seulement, pas de toast). */
export type Notification = ToastItem & { popup: boolean }

export type NotificationValue = {
  /** Tout ce qui attend encore, du plus ancien au plus récent. */
  items: Notification[]
  /** Retire pour de bon (croix de la cloche). */
  dismiss: (id: string) => void
}

export const NotificationContext = createContext<NotificationValue | null>(null)

export function useNotifications() {
  const ctx = useContext(NotificationContext)
  if (!ctx) throw new Error('useNotifications doit être utilisé dans NotificationCenter')
  return ctx
}
