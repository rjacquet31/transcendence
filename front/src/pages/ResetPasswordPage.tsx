import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { resetPassword } from '../lib/actions.ts'
import { ApiError } from '../lib/apiClient.ts'
import page from '../style/page.module.css'
import s from './ResetPasswordPage.module.css'

type ResetPasswordPageProps = {
  onNavigate: (action: string) => void
}

export function ResetPasswordPage({ onNavigate }: ResetPasswordPageProps) {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { openModal, isLoggedIn, logout } = useAuthModal()

  const [password, setPassword] = useState('')
  const [confirmedPassword, setConfirmedPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    if (!token) return
    setFieldErrors({})
    setGlobalError(null)

    if (password !== confirmedPassword) {
      setFieldErrors({ confirmedPassword: 'Les mots de passe ne correspondent pas.' })
      return
    }

    setPending(true)
    try {
      const res = await resetPassword(token, password, confirmedPassword)
      // The back revoked every session: drop the one this browser may still hold.
      if (isLoggedIn) await logout().catch(() => {})
      setSuccess(res.message)
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setFieldErrors(err.fieldErrors)
      } else {
        setGlobalError(err instanceof ApiError ? err.message : 'Erreur inconnue.')
      }
    } finally {
      setPending(false)
    }
  }

  return (
    <AppLayout current="nav:reset-password" onNavigate={onNavigate}>
      <PageHeader
        title="Nouveau mot de passe"
        subtitle="Choisis un nouveau mot de passe pour ton compte."
      />

      <div className={s.card}>
        {!token ? (
          <>
            <p className={s.globalError} role="alert">
              Ce lien de réinitialisation est incomplet.
            </p>
            <div className={s.actions}>
              <Button variant="primary" onClick={() => openModal('forgot')}>
                Demander un nouveau lien
              </Button>
            </div>
          </>
        ) : success ? (
          <>
            <p className={s.success} role="status">
              {success}
            </p>
            <div className={s.actions}>
              <Button variant="primary" onClick={() => openModal('login')}>
                Se connecter
              </Button>
            </div>
          </>
        ) : (
          <form className={s.form} onSubmit={submit} noValidate>
            <PasswordField
              id="reset-password"
              label="Nouveau mot de passe"
              value={password}
              error={fieldErrors.password}
              onChange={setPassword}
            />
            <PasswordField
              id="reset-confirm"
              label="Confirmer le mot de passe"
              value={confirmedPassword}
              error={fieldErrors.confirmedPassword}
              onChange={setConfirmedPassword}
            />

            {globalError && (
              <p className={s.globalError} role="alert">
                {globalError}
              </p>
            )}

            <div className={s.actions}>
              <Button type="submit" variant="primary" disabled={pending}>
                {pending ? 'Enregistrement…' : 'Changer le mot de passe'}
              </Button>
              {globalError && (
                <Button onClick={() => openModal('forgot')}>Demander un nouveau lien</Button>
              )}
            </div>
          </form>
        )}
      </div>
    </AppLayout>
  )
}

type PasswordFieldProps = {
  id: string
  label: string
  value: string
  error?: string
  onChange: (value: string) => void
}

function PasswordField({ id, label, value, error, onChange }: PasswordFieldProps) {
  return (
    <div className={page.field}>
      <label className={page.label} htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className={error ? `${page.input} ${s.input} ${s.inputError}` : `${page.input} ${s.input}`}
        type="password"
        value={value}
        required
        autoComplete="new-password"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && (
        <p id={`${id}-error`} className={s.error}>
          {error}
        </p>
      )}
    </div>
  )
}
