import { useState } from 'react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { errorMessage } from '../lib/apiClient.ts'
import { deleteMyAccount } from '../lib/gdpr.ts'
import page from '../style/page.module.css'
import s from './SettingsPage.module.css'

type Busy = 'export' | 'delete' | null

type SettingsPageProps = {
  onNavigate: (action: string) => void
}

export function SettingsPage({ onNavigate }: SettingsPageProps) {
  const { isLoggedIn, authChecked, user } = useAuthModal()

  const [busy, setBusy] = useState<Busy>(null)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const [deleteOpen, setDeleteOpen] = useState(false)
  const [confirmName, setConfirmName] = useState('')

  const userName = user?.userName ?? ''
  const canDelete = confirmName.trim() === userName && userName.length > 0

  const reset = () => {
    setError(null)
    setNotice(null)
  }

  const handleDelete = async () => {
    reset()
    setBusy('delete')
    try {
      await deleteMyAccount()
      window.location.assign('/')
    } catch (err) {
      setError(errorMessage(err, 'Suppression impossible pour le moment.'))
      setBusy(null)
    }
  }

  return (
    <AppLayout current="nav:settings" onNavigate={onNavigate}>
      <PageHeader
        title="Paramètres"
        subtitle="Tes données, tes droits et les documents légaux du service."
      />

      {!authChecked ? null : !isLoggedIn ? (
        <p className={page.denied}>Connecte-toi pour accéder à tes paramètres.</p>
      ) : (
        <>
          {error && <p className={s.error}>{error}</p>}
          {notice && <p className={s.notice}>{notice}</p>}

          <section className={s.section} aria-labelledby="settings-delete-title">
            <SectionTitle id="settings-delete-title">Supprimer mon compte</SectionTitle>
            <div className={`${s.card} ${s.cardDanger}`}>
              <p className={s.text}>
                Ton compte, ton avatar, tes relations et tes messages sont effacés définitivement.
                Tes parties d’échecs sont conservées sous une identité anonyme pour ne pas fausser
                l’historique de tes adversaires. Aucune récupération n’est possible.
              </p>

              {!deleteOpen ? (
                <div className={s.actions}>
                  <Button
                    disabled={busy !== null}
                    onClick={() => {
                      reset()
                      setConfirmName('')
                      setDeleteOpen(true)
                    }}
                  >
                    Supprimer mon compte
                  </Button>
                </div>
              ) : (
                <div className={s.confirm}>
                  <label className={s.label} htmlFor="settings-confirm-name">
                    Écris <span className={s.strong}>{userName}</span> pour confirmer
                  </label>
                  <input
                    id="settings-confirm-name"
                    className={`${page.input} ${s.input}`}
                    type="text"
                    value={confirmName}
                    autoComplete="off"
                    onChange={(event) => setConfirmName(event.target.value)}
                  />
                  <div className={s.actions}>
                    <Button
                      variant="primary"
                      disabled={!canDelete || busy !== null}
                      onClick={handleDelete}
                    >
                      {busy === 'delete' ? 'Suppression…' : 'Supprimer définitivement'}
                    </Button>
                    <Button
                      variant="ghost"
                      disabled={busy !== null}
                      onClick={() => {
                        setDeleteOpen(false)
                        setConfirmName('')
                      }}
                    >
                      Annuler
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </section>

          <section className={s.section} aria-labelledby="settings-legal-title">
            <SectionTitle id="settings-legal-title">Documents légaux</SectionTitle>
            <div className={s.card}>
              <p className={s.text}>
                Ce que le service collecte, pourquoi, et ce que tu acceptes en l’utilisant.
              </p>
              <div className={s.actions}>
                <Button onClick={() => onNavigate('nav:privacy')}>
                  Politique de confidentialité
                </Button>
                <Button onClick={() => onNavigate('nav:terms')}>Conditions d’utilisation</Button>
              </div>
            </div>
          </section>
        </>
      )}
    </AppLayout>
  )
}