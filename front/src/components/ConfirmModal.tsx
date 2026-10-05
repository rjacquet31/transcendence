import type { ReactNode } from 'react'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import s from './ConfirmModal.module.css'

type ConfirmModalProps = {
  open: boolean
  title: string
  /** Ce qu'on s'apprête à faire, en clair : l'action est irréversible. */
  children: ReactNode
  confirmLabel: string
  /** Ce que dit le bouton pendant la requête. */
  pendingLabel: string
  pending?: boolean
  error?: string | null
  onConfirm: () => void
  onCancel: () => void
}

/** Demande de confirmation avant une action destructive. */
export function ConfirmModal({
  open,
  title,
  children,
  confirmLabel,
  pendingLabel,
  pending = false,
  error,
  onConfirm,
  onCancel,
}: ConfirmModalProps) {
  // Focus sur « Annuler » : rien de destructif sous Entrée.
  const dialog = useDialog(open, 'button', onCancel)

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="confirm-modal-title">
      <h2 id="confirm-modal-title" className={s.title}>
        {title}
      </h2>

      <div className={s.body}>{children}</div>

      {error && (
        <p className={s.globalError} role="alert">
          {error}
        </p>
      )}

      <div className={s.actions}>
        <Button variant="ghost" onClick={onCancel} disabled={pending}>
          Annuler
        </Button>
        <Button variant="primary" onClick={onConfirm} disabled={pending}>
          {pending ? pendingLabel : confirmLabel}
        </Button>
      </div>
    </dialog>
  )
}
