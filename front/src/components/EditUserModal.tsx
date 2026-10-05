import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import type { UpdateUserPayload, UserPublicProfileDto } from '../lib/actions.ts'
import s from './EditUserModal.module.css'

/** Les rôles connus du back, et le libellé montré dans le select. */
const ROLES = [
  { value: 'USER', label: 'User' },
  { value: 'ADMIN', label: 'Admin' },
]

const EMPTY_FORM: UpdateUserPayload = { userName: '', email: '', role: 'USER' }

type EditUserModalProps = {
  /** Le compte à éditer, ou `null` quand la modale est fermée. */
  user: UserPublicProfileDto | null
  pending?: boolean
  /** Refus du back ; la modale reste ouverte. */
  error?: string | null
  onClose: () => void
  onSave: (idUser: string, values: UpdateUserPayload) => void
}

/** L'UUID est affiché mais jamais éditable : c'est la clé du compte. */
export function EditUserModal({ user, pending = false, error, onClose, onSave }: EditUserModalProps) {
  const dialog = useDialog(user !== null, '#edit-user-name', onClose)
  const [form, setForm] = useState<UpdateUserPayload>(EMPTY_FORM)

  // Repartir des valeurs du compte, sinon la modale rouvrirait sur les saisies du compte précédent.
  useEffect(() => {
    if (user) setForm({ userName: user.userName, email: user.email ?? '', role: user.role ?? 'USER' })
  }, [user])

  const update = (field: keyof UpdateUserPayload, value: string) => {
    setForm((previous) => ({ ...previous, [field]: value }))
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (user) onSave(user.id, form)
  }

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="edit-user-title">
      <div className={s.head}>
        <h2 id="edit-user-title" className={s.title}>
          Modifier le compte
        </h2>
        <Button variant="ghost" ariaLabel="Fermer" onClick={onClose}>
          <X size={18} />
        </Button>
      </div>

      <form className={s.form} onSubmit={submit} noValidate>
        <div className={s.field}>
          <span className={s.label}>Identifiant</span>
          <p className={s.readonly}>{user?.id ?? '—'}</p>
        </div>

        <div className={s.field}>
          <label className={s.label} htmlFor="edit-user-name">
            Pseudo
          </label>
          <input
            id="edit-user-name"
            className={s.input}
            type="text"
            value={form.userName}
            autoComplete="off"
            onChange={(event) => update('userName', event.target.value)}
          />
        </div>

        <div className={s.field}>
          <label className={s.label} htmlFor="edit-user-email">
            E-mail
          </label>
          <input
            id="edit-user-email"
            className={s.input}
            type="email"
            value={form.email}
            autoComplete="off"
            onChange={(event) => update('email', event.target.value)}
          />
        </div>

        <div className={s.field}>
          <label className={s.label} htmlFor="edit-user-role">
            Rôle
          </label>
          <select
            id="edit-user-role"
            className={s.select}
            value={form.role}
            onChange={(event) => update('role', event.target.value)}
          >
            {ROLES.map((role) => (
              <option key={role.value} value={role.value}>
                {role.label}
              </option>
            ))}
          </select>
        </div>

        {error && (
          <p className={s.globalError} role="alert">
            {error}
          </p>
        )}

        <div className={s.actions}>
          <Button variant="ghost" onClick={onClose} disabled={pending}>
            Annuler
          </Button>
          <Button type="submit" variant="primary" disabled={pending}>
            {pending ? 'Enregistrement…' : 'Sauvegarder'}
          </Button>
        </div>
      </form>
    </dialog>
  )
}
