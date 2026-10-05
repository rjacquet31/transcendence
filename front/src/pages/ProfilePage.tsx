import { useEffect, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { TwoFactorModal } from '../components/TwoFactorModal.tsx'
import { UserAvatar } from '../components/UserAvatar.tsx'
import {
  disableTwoFactor,
  enableTwoFactor,
  link42Account,
  setupTwoFactor,
  unlink42Account,
  updateMyProfile,
  uploadMyAvatar,
} from '../lib/actions.ts'
import type { TwoFactorSetupDto } from '../lib/actions.ts'
import { ApiError, errorMessage } from '../lib/apiClient.ts'
import page from '../style/page.module.css'
import s from './ProfilePage.module.css'

const USERNAME_RE = /^[A-Za-z0-9_-]{3,20}$/
const EMAIL_RE = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$/
const AVATAR_MAX_BYTES = 2 * 1024 * 1024
const AVATAR_TYPES = ['image/png', 'image/jpeg']

/** Ce que le back peut nous renvoyer en query string au retour d'un liage 42. */
const LINK_MESSAGES: Record<string, string> = {
  already_linked: 'Ce compte 42 est déjà rattaché à un compte.',
  link_failed: 'La liaison a échoué : relance-la depuis cette page.',
  oauth_failed: 'L’intra 42 n’a pas répondu. Réessaie dans un instant.',
  oauth_denied: 'Liaison avec 42 annulée.',
}

function codeErrorOf(err: unknown, fallback: string) {
  return err instanceof ApiError ? (err.fieldErrors.code ?? err.message) : fallback
}

type ProfilePageProps = {
  onNavigate: (action: string) => void
}

export function ProfilePage({ onNavigate }: ProfilePageProps) {
  const { user, isLoggedIn, authChecked, refreshUser } = useAuthModal()
  const [searchParams, setSearchParams] = useSearchParams()

  const [userName, setUserName] = useState('')
  const [email, setEmail] = useState('')
  const [savingId, setSavingId] = useState(false)
  const [idErrors, setIdErrors] = useState<Record<string, string>>({})
  const [idError, setIdError] = useState<string | null>(null)
  const [idNotice, setIdNotice] = useState<string | null>(null)

  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const [avatarError, setAvatarError] = useState<string | null>(null)
  const [avatarNotice, setAvatarNotice] = useState<string | null>(null)

  const [linkPending, setLinkPending] = useState(false)
  const [linkError, setLinkError] = useState<string | null>(null)
  const [linkNotice, setLinkNotice] = useState<string | null>(null)

  const [twoFactorMode, setTwoFactorMode] = useState<'enable' | 'disable' | null>(null)
  const [twoFactorSetup, setTwoFactorSetup] = useState<TwoFactorSetupDto | null>(null)
  const [twoFactorPending, setTwoFactorPending] = useState(false)
  const [twoFactorError, setTwoFactorError] = useState<string | null>(null)
  const [twoFactorNotice, setTwoFactorNotice] = useState<string | null>(null)

  useEffect(() => {
    if (user) {
      setUserName(user.userName)
      setEmail(user.email ?? '')
    }
  }, [user])

  // Retour du liage 42 : on affiche le résultat une fois, puis on nettoie l'URL.
  useEffect(() => {
    const linked = searchParams.get('linked')
    const error = searchParams.get('error')
    if (!linked && !error) return

    if (linked) setLinkNotice('Compte 42 rattaché.')
    if (error) setLinkError(LINK_MESSAGES[error] ?? 'La liaison a échoué.')
    setSearchParams({}, { replace: true })
  }, [searchParams, setSearchParams])

  useEffect(() => {
    if (!file) {
      setPreview(null)
      return
    }
    const url = URL.createObjectURL(file)
    setPreview(url)
    return () => URL.revokeObjectURL(url)
  }, [file])

  const dirty = user !== null && (userName !== user.userName || email !== user.email)

  const pickAvatar = (event: ChangeEvent<HTMLInputElement>) => {
    setAvatarError(null)
    setAvatarNotice(null)

    const picked = event.target.files?.[0] ?? null
    if (!picked) {
      setFile(null)
      return
    }
    if (!AVATAR_TYPES.includes(picked.type)) {
      setAvatarError('Formats acceptés : PNG ou JPEG.')
      setFile(null)
      return
    }
    if (picked.size > AVATAR_MAX_BYTES) {
      setAvatarError('Image trop lourde : 2 Mo maximum.')
      setFile(null)
      return
    }
    setFile(picked)
  }

  const submitAvatar = async (event: FormEvent) => {
    event.preventDefault()
    if (!file) return

    setUploading(true)
    setAvatarError(null)
    setAvatarNotice(null)
    try {
      await uploadMyAvatar(file)
      await refreshUser()
      setFile(null)
      setAvatarNotice('Photo de profil mise à jour.')
    } catch (err) {
      setAvatarError(errorMessage(err, 'Envoi de l’image impossible.'))
    } finally {
      setUploading(false)
    }
  }

  const submitIdentity = async (event: FormEvent) => {
    event.preventDefault()
    if (!user) return

    const name = userName.trim()
    const mail = email.trim()

    const next: Record<string, string> = {}
    if (!USERNAME_RE.test(name)) next.userName = '3 à 20 caractères : lettres, chiffres, _ ou -.'
    if (!EMAIL_RE.test(mail)) next.email = 'Adresse e-mail invalide.'
    setIdErrors(next)
    setIdError(null)
    setIdNotice(null)
    if (Object.keys(next).length > 0) return

    setSavingId(true)
    try {
      await updateMyProfile(user.id, { userName: name, email: mail })
      await refreshUser()
      setIdNotice('Profil enregistré.')
    } catch (err) {
      if (err instanceof ApiError && Object.keys(err.fieldErrors).length > 0) {
        setIdErrors(err.fieldErrors)
      } else {
        setIdError(errorMessage(err, 'Enregistrement impossible.'))
      }
    } finally {
      setSavingId(false)
    }
  }

  const startEnable = async () => {
    setTwoFactorPending(true)
    setTwoFactorError(null)
    setTwoFactorNotice(null)
    try {
      setTwoFactorSetup(await setupTwoFactor())
      setTwoFactorMode('enable')
    } catch (err) {
      setTwoFactorError(errorMessage(err, 'Impossible de générer le QR code.'))
    } finally {
      setTwoFactorPending(false)
    }
  }

  const startDisable = () => {
    setTwoFactorError(null)
    setTwoFactorNotice(null)
    setTwoFactorMode('disable')
  }

  const closeTwoFactor = () => {
    setTwoFactorMode(null)
    setTwoFactorSetup(null)
    setTwoFactorError(null)
  }

  const submitTwoFactor = async (code: string) => {
    const enabling = twoFactorMode === 'enable'

    setTwoFactorPending(true)
    setTwoFactorError(null)
    try {
      await (enabling ? enableTwoFactor(code) : disableTwoFactor(code))
      await refreshUser()
      closeTwoFactor()
      setTwoFactorNotice(enabling ? 'A2F activée.' : 'A2F désactivée.')
    } catch (err) {
      setTwoFactorError(codeErrorOf(err, 'Vérification impossible.'))
    } finally {
      setTwoFactorPending(false)
    }
  }

  const startLink = () => {
    setLinkError(null)
    setLinkNotice(null)
    setLinkPending(true)
    link42Account()
  }

  const startUnlink = async () => {
    setLinkError(null)
    setLinkNotice(null)
    setLinkPending(true)
    try {
      await unlink42Account()
      await refreshUser()
      setLinkNotice('Compte 42 détaché.')
    } catch (err) {
      setLinkError(errorMessage(err, 'Impossible de détacher le compte 42.'))
    } finally {
      setLinkPending(false)
    }
  }

  return (
    <AppLayout current="nav:profile" onNavigate={onNavigate}>
      <PageHeader title="Profil" subtitle="Ta photo, ton pseudo et ton adresse e-mail." />

      {!authChecked ? null : !isLoggedIn || !user ? (
        <p className={page.denied}>Connecte-toi pour accéder à ton profil.</p>
      ) : (
        <>
          <section className={s.section} aria-labelledby="profile-avatar-title">
            <SectionTitle id="profile-avatar-title">Photo de profil</SectionTitle>
            <form className={s.avatarForm} onSubmit={submitAvatar}>
              {preview ? (
                <img
                  className={s.avatar}
                  src={preview}
                  alt="Aperçu du nouvel avatar"
                  width={96}
                  height={96}
                />
              ) : (
                <UserAvatar userName={user.userName} avatarUrl={user.avatarUrl} size={96} />
              )}
              <div className={s.avatarControls}>
                <label className={s.fileLabel}>
                  <input
                    className={s.fileInput}
                    type="file"
                    accept="image/png,image/jpeg"
                    onChange={pickAvatar}
                  />
                  {file ? file.name : 'Choisir une image'}
                </label>
                <p className={s.hint}>PNG ou JPEG, 2 Mo maximum.</p>
                <Button type="submit" variant="primary" disabled={!file || uploading}>
                  {uploading ? 'Envoi…' : 'Téléverser'}
                </Button>
                {avatarError && <p className={s.error}>{avatarError}</p>}
                {avatarNotice && <p className={s.notice}>{avatarNotice}</p>}
              </div>
            </form>
          </section>

          <section className={s.section} aria-labelledby="profile-identity-title">
            <SectionTitle id="profile-identity-title">Identité</SectionTitle>
            <form className={s.form} onSubmit={submitIdentity} noValidate>
              <div className={page.field}>
                <label className={page.label} htmlFor="profile-username">
                  Pseudo
                </label>
                <input
                  id="profile-username"
                  className={idErrors.userName ? `${page.input} ${s.input} ${s.inputError}` : `${page.input} ${s.input}`}
                  type="text"
                  value={userName}
                  autoComplete="username"
                  onChange={(event) => setUserName(event.target.value)}
                />
                {idErrors.userName && <p className={s.error}>{idErrors.userName}</p>}
              </div>

              <div className={page.field}>
                <label className={page.label} htmlFor="profile-email">
                  E-mail
                </label>
                <input
                  id="profile-email"
                  className={idErrors.email ? `${page.input} ${s.input} ${s.inputError}` : `${page.input} ${s.input}`}
                  type="email"
                  value={email}
                  autoComplete="email"
                  onChange={(event) => setEmail(event.target.value)}
                />
                {idErrors.email && <p className={s.error}>{idErrors.email}</p>}
              </div>

              {idError && <p className={s.error}>{idError}</p>}
              {idNotice && <p className={s.notice}>{idNotice}</p>}

              <div className={s.actions}>
                <Button type="submit" variant="primary" disabled={savingId || !dirty}>
                  {savingId ? 'Enregistrement…' : 'Enregistrer'}
                </Button>
              </div>
            </form>
          </section>

          <section className={s.section} aria-labelledby="profile-2fa-title">
            <SectionTitle id="profile-2fa-title">Sécurité</SectionTitle>
            <p className={s.hint}>
              Authentification à deux facteurs : {user.twoFactorEnabled ? 'activée' : 'désactivée'}.
            </p>
            <div className={s.twoFactorActions}>
              <Button
                variant="primary"
                onClick={startEnable}
                disabled={twoFactorPending || user.twoFactorEnabled === true}
              >
                Activer l’A2F
              </Button>
              <Button onClick={startDisable} disabled={twoFactorPending || !user.twoFactorEnabled}>
                Désactiver l’A2F
              </Button>
            </div>
            {twoFactorError && !twoFactorMode && <p className={s.error}>{twoFactorError}</p>}
            {twoFactorNotice && <p className={s.notice}>{twoFactorNotice}</p>}
          </section>

          <section className={s.section} aria-labelledby="profile-42-title">
            <SectionTitle id="profile-42-title">Compte 42</SectionTitle>
            <p className={s.hint}>
              {user.oauth42Linked
                ? 'Un compte 42 est rattaché : tu peux te connecter d’un clic depuis l’écran d’accueil.'
                : 'Aucun compte 42 rattaché. Le lier te permettra de te connecter via l’intra, même si ton e-mail ici diffère du sien.'}
            </p>
            <div className={s.linkActions}>
              <Button variant="42" onClick={startLink} disabled={linkPending || user.oauth42Linked === true}>
                {linkPending ? 'Redirection…' : 'Lier mon compte 42'}
              </Button>
              <Button onClick={startUnlink} disabled={linkPending || !user.oauth42Linked}>
                Délier
              </Button>
            </div>
            {linkError && <p className={s.error}>{linkError}</p>}
            {linkNotice && <p className={s.notice}>{linkNotice}</p>}
          </section>

          <TwoFactorModal
            key={twoFactorMode ?? 'closed'}
            mode={twoFactorMode}
            setup={twoFactorSetup}
            pending={twoFactorPending}
            error={twoFactorError}
            onSubmit={submitTwoFactor}
            onClose={closeTwoFactor}
          />
        </>
      )}
    </AppLayout>
  )
}