import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthModal } from '../components/AuthModal.tsx'
import type { LoginForm, SignupForm } from '../components/AuthModal.tsx'
import { TwoFactorModal } from '../components/TwoFactorModal.tsx'
import { getMyProfile, loginWith42 } from '../lib/actions.ts'
import type { UserPublicProfileDto } from '../lib/actions.ts'
import { api, ApiError, errorMessage } from '../lib/apiClient.ts'
import { AuthModalContext } from './authModalContext.ts'
import type { AuthMode } from './authModalContext.ts'

/** /auth/login répond "Login successful" (texte), ou ce JSON si le compte attend un code A2F. */
type LoginResponse = string | { twoFactorRequired: true }

const QUERY = new URLSearchParams(window.location.search)

/** Retour d'OAuth 42 sur un compte A2F : le back redirige avec `?twoFactor=required`. */
const OAUTH_TWO_FACTOR = QUERY.get('twoFactor') === 'required'

/** Retour d'OAuth 42 en échec. Le back refuse de rattacher un compte 42 à un compte
 *  local de même e-mail : l'e-mail du register n'étant pas vérifié, la fusion
 *  automatique offrirait le compte au premier inscrit. */
const OAUTH_ERROR = QUERY.get('error')

const OAUTH_ERRORS: Record<string, string> = {
  email_already_registered:
    'Un compte existe déjà avec cet e-mail. Connecte-toi avec ton mot de passe, puis lie ton compte 42 depuis ton profil.',
  oauth_failed: 'L’intra 42 n’a pas répondu. Réessaie dans un instant.',
  oauth_denied: 'Connexion avec 42 annulée.',
}

const OAUTH_NOTICE = OAUTH_ERROR ? (OAUTH_ERRORS[OAUTH_ERROR] ?? null) : null

export function AuthModalProvider({ children }: { children: ReactNode }) {
  // Un retour 42 en échec ouvre la modale d'emblée, sur le message du back.
  const [isOpen, setIsOpen] = useState(OAUTH_NOTICE !== null)
  const [mode, setMode] = useState<AuthMode>('login')
  const [user, setUser] = useState<UserPublicProfileDto | null>(null)
  // Tant que la session n'est pas vérifiée, l'UI n'affiche ni « connecté » ni « invité ».
  const [authChecked, setAuthChecked] = useState(false)
  // Identifiants gardés le temps de saisir le code A2F, puis renvoyés avec lui ;
  // 'oauth' quand le back tient déjà l'utilisateur (cookie two_factor_token).
  const [twoFactorLogin, setTwoFactorLogin] = useState<LoginForm | 'oauth' | null>(
    OAUTH_TWO_FACTOR ? 'oauth' : null,
  )
  const [twoFactorPending, setTwoFactorPending] = useState(false)
  const [twoFactorError, setTwoFactorError] = useState<string | null>(null)
  // Message du back au retour d'un login 42 raté : la modale s'ouvre dessus.
  const [oauthNotice, setOauthNotice] = useState<string | null>(OAUTH_NOTICE)

  // Restauration de session : le cookie httpOnly survit au rechargement de la page.
  useEffect(() => {
    if (OAUTH_TWO_FACTOR || OAUTH_ERROR) {
      window.history.replaceState(null, '', window.location.pathname)
    }

    let cancelled = false
    getMyProfile()
      .then((me) => {
        if (!cancelled) setUser(me)
      })
      .catch(() => {
        if (!cancelled) setUser(null)
      })
      .finally(() => {
        if (!cancelled) setAuthChecked(true)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const openModal = (next: AuthMode = 'login') => {
    setOauthNotice(null)
    setMode(next)
    setIsOpen(true)
  }

  const closeModal = () => {
    setIsOpen(false)
    setOauthNotice(null)
  }

  const logout = async () => {
    try {
      await api.post<string>('/auth/logout')
    } finally {
      setUser(null)
    }
  }

  /** Après une édition de profil : on relit la source de vérité côté back. */
  const refreshUser = async () => {
    setUser(await getMyProfile())
  }

  /** Ouvre la session : le back pose les cookies, on lit le profil dans la foulée.
   *  Si le compte a l'A2F, pas de cookie : on passe à la modale du code. */
  const login = async (values: LoginForm) => {
    const res = await api.post<LoginResponse>('/auth/login', values)
    setIsOpen(false)
    if (typeof res === 'object' && res.twoFactorRequired) {
      setTwoFactorLogin(values)
      return
    }
    setUser(await getMyProfile())
  }

  const closeTwoFactor = () => {
    setTwoFactorLogin(null)
    setTwoFactorError(null)
  }

  /** Second envoi : mêmes identifiants + code, ou le code seul après OAuth 42. */
  const submitTwoFactor = async (code: string) => {
    if (!twoFactorLogin) return
    setTwoFactorPending(true)
    setTwoFactorError(null)
    try {
      await (twoFactorLogin === 'oauth'
        ? api.post('/auth/2fa/login', { code })
        : api.post('/auth/login', { ...twoFactorLogin, code }))
      setUser(await getMyProfile())
      closeTwoFactor()
    } catch (err) {
      const fieldError = err instanceof ApiError ? err.fieldErrors.code : null
      setTwoFactorError(fieldError ?? errorMessage(err, 'Vérification impossible.'))
    } finally {
      setTwoFactorPending(false)
    }
  }

  /** /register renvoie un 201 sans cookie : on enchaîne pour ouvrir la session. */
  const signup = async (values: SignupForm) => {
    await api.post<void>('/auth/register', values)
    await login({ email: values.email, password: values.password })
  }

  const value = {
    isOpen,
    mode,
    isLoggedIn: user !== null,
    user,
    authChecked,
    openModal,
    closeModal,
    logout,
    refreshUser,
  }

  return (
    <AuthModalContext.Provider value={value}>
      {children}
      <AuthModal
        isOpen={isOpen}
        mode={mode}
        onModeChange={setMode}
        onClose={closeModal}
        onSignup={signup}
        onLogin={login}
        onOAuth42={loginWith42}
        notice={oauthNotice}
      />
      {/* La clé vide le champ du code à chaque nouvelle connexion. */}
      <TwoFactorModal
        key={twoFactorLogin ? 'open' : 'closed'}
        mode={twoFactorLogin ? 'login' : null}
        pending={twoFactorPending}
        error={twoFactorError}
        onSubmit={submitTwoFactor}
        onClose={closeTwoFactor}
      />
    </AuthModalContext.Provider>
  )
}
