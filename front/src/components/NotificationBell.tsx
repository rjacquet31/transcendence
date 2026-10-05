import { useId } from 'react'
import { useNotifications } from '../ws/notificationContext.ts'
import { ToastCard } from './Toast.tsx'
import s from './NotificationBell.module.css'

/** Cloche de la navbar : tout ce qui attend encore, via un `popover` natif (fermeture au clic extérieur et Échap). */
export function NotificationBell() {
  const { items, dismiss } = useNotifications()
  const panelId = useId()
  const count = items.length

  return (
    <>
      <button
        type="button"
        className={s.bell}
        popoverTarget={panelId}
        aria-label={count > 0 ? `Notifications (${count})` : 'Notifications'}
      >
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
          <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </svg>
        {count > 0 && <span className={s.badge}>{count > 9 ? '9+' : count}</span>}
      </button>

      <div id={panelId} popover="auto" className={s.panel}>
        <p className={s.heading}>Notifications</p>
        {count === 0 ? (
          <p className={s.empty}>Rien de nouveau.</p>
        ) : (
          items.map((item) => <ToastCard key={item.id} item={item} onDismiss={dismiss} />)
        )}
      </div>
    </>
  )
}
