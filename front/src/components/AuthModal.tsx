import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import type { AuthMode } from '../auth/authModalContext.ts'
import { ApiError } from '../lib/apiClient.ts'
import { forgotPassword } from '../lib/actions.ts'
import s from './AuthModal.module.css'

export type SignupForm = {
  userName: string
  email: string
  password: string
  confirmedPassword: string
}

export type LoginForm = {
  email: string
  password: string
}

const EMPTY_SIGNUP: SignupForm = { userName: '', email: '', password: '', confirmedPassword: '' }
const EMPTY_LOGIN: LoginForm = { email: '', password: '' }

const WORDING = {
  signup: {
    title: 'Rejoindre la table',
    submit: "S'inscrire",
    submitting: 'Création…',
    oauth42: "S'inscrire via 42",
    firstField: '#signup-username',
  },
  login: {
    title: 'Reprendre la partie',
    submit: 'Se connecter',
    submitting: 'Connexion…',
    oauth42: 'Se connecter via 42',
    firstField: '#login-email',
  },
  forgot: {
    title: 'Mot de passe oublié',
    submit: 'Envoyer le lien',
    submitting: 'Envoi…',
    oauth42: null,
    firstField: '#forgot-email',
  },
}

type AuthModalProps = {
  isOpen: boolean
  mode: AuthMode
  onModeChange: (mode: AuthMode) => void
  onClose: () => void
  onSignup: (values: SignupForm) => Promise<void>
  onLogin: (values: LoginForm) => Promise<void>
  onOAuth42?: () => void
  /** Message du back au retour d'un OAuth 42 raté. */
  notice?: string | null
}

export function AuthModal({
  isOpen,
  mode,
  onModeChange,
  onClose,
  onSignup,
  onLogin,
  onOAuth42,
  notice,
}: AuthModalProps) {
  const isSignup = mode === 'signup'
  const isForgot = mode === 'forgot'
  const wording = WORDING[mode]

  const dialog = useDialog(isOpen, wording.firstField, onClose)

  const [signup, setSignup] = useState<SignupForm>(EMPTY_SIGNUP)
  const [login, setLogin] = useState<LoginForm>(EMPTY_LOGIN)
  const [forgotEmail, setForgotEmail] = useState('')
  const [success, setSuccess] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [globalError, setGlobalError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  useEffect(() => {
    setFieldErrors({})
    setGlobalError(null)
    setSuccess(null)
  }, [isOpen, mode])

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    setFieldErrors({})
    setGlobalError(null)
    setSuccess(null)

    if (isSignup && signup.password !== signup.confirmedPassword) {
      setFieldErrors({ confirmedPassword: 'Les mots de passe ne correspondent pas.' })
      return
    }

    setPending(true)
    try {
      if (isForgot) {
        setSuccess((await forgotPassword(forgotEmail)).message)
      } else {
        await (isSignup ? onSignup(signup) : onLogin(login))
        setSignup(EMPTY_SIGNUP)
        setLogin(EMPTY_LOGIN)
      }
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setFieldErrors(err.fieldErrors)
      } else if (err instanceof ApiError) {
        setGlobalError(err.status === 401 ? 'Email ou mot de passe incorrect.' : err.message)
      } else {
        setGlobalError('Erreur inconnue.')
      }
    } finally {
      setPending(false)
    }
  }

  const updateSignup = (field: keyof SignupForm, value: string) => {
    setSignup((previous) => ({ ...previous, [field]: value }))
  }

  const updateLogin = (field: keyof LoginForm, value: string) => {
    setLogin((previous) => ({ ...previous, [field]: value }))
  }

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="auth-modal-title">
      <div className={s.head}>
        <h2 id="auth-modal-title" className={s.title}>
          {wording.title}
        </h2>
        <Button variant="ghost" ariaLabel="Fermer" onClick={onClose}>
          <X size={18} />
        </Button>
      </div>

      <div className={s.tabs} role="tablist" aria-label="Mode d'authentification">
        <Tab label="Connexion" selected={mode === 'login'} onSelect={() => onModeChange('login')} />
        <Tab label="Inscription" selected={isSignup} onSelect={() => onModeChange('signup')} />
      </div>

      <form className={s.form} onSubmit={submit} noValidate>
        {isForgot ? (
          <>
            <p className={s.hint}>
              Entre l'email de ton compte : tu recevras un lien pour choisir un nouveau mot de passe.
            </p>
            <Field
              id="forgot-email"
              label="Email"
              type="email"
              value={forgotEmail}
              error={fieldErrors.email}
              onChange={setForgotEmail}
              autoComplete="email"
            />
            <div className={s.forgot}>
              <a
                href="#"
                onClick={(event) => {
                  event.preventDefault()
                  onModeChange('login')
                }}
              >
                Retour à la connexion
              </a>
            </div>
          </>
        ) : isSignup ? (
          <>
            <Field
              id="signup-username"
              label="Nom d'utilisateur"
              value={signup.userName}
              error={fieldErrors.userName}
              onChange={(value) => updateSignup('userName', value)}
              autoComplete="username"
            />
            <Field
              id="signup-email"
              label="Email"
              type="email"
              value={signup.email}
              error={fieldErrors.email}
              onChange={(value) => updateSignup('email', value)}
              autoComplete="email"
            />
            <Field
              id="signup-password"
              label="Mot de passe"
              type="password"
              value={signup.password}
              error={fieldErrors.password}
              onChange={(value) => updateSignup('password', value)}
              autoComplete="new-password"
            />
            <Field
              id="signup-confirm"
              label="Confirmer le mot de passe"
              type="password"
              value={signup.confirmedPassword}
              error={fieldErrors.confirmedPassword}
              onChange={(value) => updateSignup('confirmedPassword', value)}
              autoComplete="new-password"
            />
          </>
        ) : (
          <>
            <Field
              id="login-email"
              label="Email"
              type="email"
              value={login.email}
              error={fieldErrors.email}
              onChange={(value) => updateLogin('email', value)}
              autoComplete="email"
            />
            <Field
              id="login-password"
              label="Mot de passe"
              type="password"
              value={login.password}
              error={fieldErrors.password}
              onChange={(value) => updateLogin('password', value)}
              autoComplete="current-password"
            />
            <div className={s.forgot}>
              <a
                href="#"
                onClick={(event) => {
                  event.preventDefault()
                  setForgotEmail(login.email)
                  onModeChange('forgot')
                }}
              >
                Mot de passe oublié ?
              </a>
            </div>
          </>
        )}

        {success && (
          <p className={s.success} role="status">
            {success}
          </p>
        )}

        {(globalError ?? notice) && (
          <p className={s.globalError} role="alert">
            {globalError ?? notice}
          </p>
        )}

        <div className={s.submit}>
          <Button type="submit" variant="primary" fullWidth disabled={pending}>
            {pending ? wording.submitting : wording.submit}
          </Button>
          {wording.oauth42 && (
            <Button variant="42" fullWidth disabled={pending} onClick={onOAuth42}>
              {wording.oauth42}
            </Button>
          )}
        </div>
      </form>
    </dialog>
  )
}

type TabProps = {
  label: string
  selected: boolean
  onSelect: () => void
}

function Tab({ label, selected, onSelect }: TabProps) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={selected}
      className={selected ? `${s.tab} ${s.tabActive}` : s.tab}
      onClick={onSelect}
    >
      {label}
    </button>
  )
}

type FieldProps = {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
  error?: string
  autoComplete?: string
}

function Field({ id, label, value, onChange, type = 'text', error, autoComplete }: FieldProps) {
  return (
    <div className={s.field}>
      <label className={s.label} htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className={error ? `${s.input} ${s.inputError}` : s.input}
        type={type}
        value={value}
        required
        autoComplete={autoComplete}
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
