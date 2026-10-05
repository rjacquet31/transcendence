import { useState, type FormEvent } from 'react'
import { X } from 'lucide-react'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import type { TwoFactorSetupDto } from '../lib/actions.ts'
import s from './TwoFactorModal.module.css'

export type TwoFactorMode = 'enable' | 'disable' | 'login'

const WORDING = {
  enable: { title: 'Activer l’A2F', submit: 'Activer' },
  disable: { title: 'Désactiver l’A2F', submit: 'Désactiver' },
  login: { title: 'Vérification A2F', submit: 'Valider' },
}

type TwoFactorModalProps = {
  /** `null` = fermée. */
  mode: TwoFactorMode | null
  /** QR code et secret à afficher en mode `enable`. */
  setup?: TwoFactorSetupDto | null
  pending?: boolean
  /** Refus du back ; la modale reste ouverte. */
  error?: string | null
  onSubmit: (code: string) => void
  onClose: () => void
}

/** Saisie d'un code A2F, précédée du QR code en mode activation. */
export function TwoFactorModal({ mode, setup, pending = false, error, onSubmit, onClose }: TwoFactorModalProps) {
  const dialog = useDialog(mode !== null, '#two-factor-code', onClose)
  const [code, setCode] = useState('')
  const enabling = mode === 'enable'
  const wording = WORDING[mode ?? 'login']

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit(code)
  }

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="two-factor-title">
      <div className={s.head}>
        <h2 id="two-factor-title" className={s.title}>
          {wording.title}
        </h2>
        <Button variant="ghost" ariaLabel="Fermer" onClick={onClose}>
          <X size={18} />
        </Button>
      </div>

      <form className={s.form} onSubmit={submit} noValidate>
        {enabling && setup && (
          <>
            <p className={s.text}>Scanne ce QR code avec ton application d’authentification.</p>
            <img className={s.qr} src={setup.qrCodeDataUri} alt="QR code A2F" width={200} height={200} />
          </>
        )}

        <div className={s.field}>
          <label className={s.label} htmlFor="two-factor-code">
            Code à 6 chiffres
          </label>
          <input
            id="two-factor-code"
            className={s.input}
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(event) => setCode(event.target.value)}
          />
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
            {pending ? 'Vérification…' : wording.submit}
          </Button>
        </div>
      </form>
    </dialog>
  )
}
