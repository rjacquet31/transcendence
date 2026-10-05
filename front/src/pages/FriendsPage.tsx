import { useState } from 'react'
import type { FormEvent } from 'react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { useChat } from '../chat/chatContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { MenuActions } from '../components/MenuActions.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { Tableau, type Colonne } from '../components/Tableau.tsx'
import { UserAvatar } from '../components/UserAvatar.tsx'
import {
  acceptFriendRequest,
  blockUser,
  declineFriendRequest,
  deleteFriend,
  sendFriendRequest,
  unblockUser,
  type FriendListDto,
} from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import { formatDateTime } from '../lib/format.ts'
import page from '../style/page.module.css'
import s from './FriendsPage.module.css'

function split(rows: FriendListDto[]) {
  const pending = rows.filter((row) => row.status === 'PENDING')
  return {
    friends: rows.filter((row) => row.status === 'ACCEPTED'),
    received: pending.filter((row) => row.senderId === row.idUser),
    sent: pending.filter((row) => row.senderId !== row.idUser),
    blocked: rows.filter((row) => row.status === 'BLOCKED'),
  }
}

type FriendsPageProps = {
  onNavigate: (action: string) => void
}

export function FriendsPage({ onNavigate }: FriendsPageProps) {
  const { isLoggedIn, authChecked } = useAuthModal()
  const { rows, loading, error, refresh, unread, avatarOf, openQuickChat } = useChat()

  const [busy, setBusy] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const [userName, setUserName] = useState('')
  const [sending, setSending] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [formNotice, setFormNotice] = useState<string | null>(null)

  const run = async (
    row: FriendListDto,
    action: (idUser: string) => Promise<void>,
    fallback: string,
  ) => {
    setBusy(row.idUser)
    setActionError(null)
    try {
      await action(row.idUser)
      await refresh()
    } catch (err) {
      setActionError(errorMessage(err, fallback))
    } finally {
      setBusy(null)
    }
  }

  const submitRequest = async (event: FormEvent) => {
    event.preventDefault()
    const target = userName.trim()
    if (!target) return

    setSending(true)
    setFormError(null)
    setFormNotice(null)
    try {
      await sendFriendRequest(target)
      setUserName('')
      setFormNotice(`Demande envoyée à ${target}.`)
      await refresh()
    } catch (err) {
      setFormError(errorMessage(err, 'Envoi de la demande impossible.'))
    } finally {
      setSending(false)
    }
  }

  const { friends, received, sent, blocked } = split(rows)

  const nameColumn: Colonne<FriendListDto> = {
    key: 'userName',
    header: 'Utilisateur',
    render: (row) => (
      <span className={s.user}>
        <UserAvatar userName={row.userName} avatarUrl={avatarOf(row.idUser)} size={30} />
        <span className={s.userName}>{row.userName}</span>
      </span>
    ),
  }

  const dateColumn = (header: string): Colonne<FriendListDto> => ({
    key: 'createAt',
    header,
    render: (row) => formatDateTime(row.createAt),
  })

  const friendColumns: Colonne<FriendListDto>[] = [
    nameColumn,
    dateColumn('Ami depuis'),
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className={s.rowActions}>
          <Button
            variant="primary"
            onClick={() => onNavigate(`nav:chat:${row.idUser}`)}
            ariaLabel={`Ouvrir la conversation avec ${row.userName}`}
          >
            {(unread[row.idUser] ?? 0) > 0 ? `Chat (${unread[row.idUser]})` : 'Chat'}
          </Button>
          <Button
            onClick={() => openQuickChat(row.idUser)}
            ariaLabel={`Ouvrir un quick chat avec ${row.userName}`}
          >
            Quick chat
          </Button>
          <MenuActions
            ariaLabel={`Actions sur ${row.userName}`}
            items={[
              {
                key: 'delete',
                label: 'Retirer',
                onSelect: () => run(row, deleteFriend, 'Suppression impossible.'),
              },
              {
                key: 'block',
                label: 'Bloquer',
                onSelect: () => run(row, blockUser, 'Blocage impossible.'),
              },
            ]}
          />
        </div>
      ),
    },
  ]

  const receivedColumns: Colonne<FriendListDto>[] = [
    nameColumn,
    dateColumn('Reçue le'),
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className={s.rowActions}>
          <Button
            variant="primary"
            disabled={busy === row.idUser}
            onClick={() => run(row, acceptFriendRequest, 'Acceptation impossible.')}
          >
            Accepter
          </Button>
          <Button
            disabled={busy === row.idUser}
            onClick={() => run(row, declineFriendRequest, 'Refus impossible.')}
          >
            Refuser
          </Button>
          <Button
            variant="ghost"
            disabled={busy === row.idUser}
            onClick={() => run(row, blockUser, 'Blocage impossible.')}
          >
            Bloquer
          </Button>
        </div>
      ),
    },
  ]

  const sentColumns: Colonne<FriendListDto>[] = [
    nameColumn,
    dateColumn('Envoyée le'),
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className={s.rowActions}>
          <Button
            variant="ghost"
            disabled={busy === row.idUser}
            onClick={() => run(row, declineFriendRequest, 'Annulation impossible.')}
          >
            Annuler
          </Button>
        </div>
      ),
    },
  ]

  const blockedColumns: Colonne<FriendListDto>[] = [
    nameColumn,
    dateColumn('Bloqué le'),
    {
      key: 'actions',
      header: 'Actions',
      render: (row) => (
        <div className={s.rowActions}>
          <Button
            variant="ghost"
            disabled={busy === row.idUser}
            onClick={() => run(row, unblockUser, 'Déblocage impossible.')}
            ariaLabel={`Débloquer ${row.userName}`}
          >
            Débloquer
          </Button>
        </div>
      ),
    },
  ]

  return (
    <AppLayout current="nav:friends" onNavigate={onNavigate}>
      <PageHeader
        title="Amis"
        subtitle="Tes amis, tes demandes en attente et les comptes que tu as bloqués."
      />

      {!authChecked ? null : !isLoggedIn ? (
        <p className={page.denied}>Connecte-toi pour voir ta liste d’amis.</p>
      ) : (
        <>
          <section aria-labelledby="friends-add-title">
            <SectionTitle id="friends-add-title">Ajouter un ami</SectionTitle>
            <form className={s.form} onSubmit={submitRequest}>
              <label className={page.label} htmlFor="friend-username">
                Pseudo exact du joueur
              </label>
              <div className={s.formRow}>
                <input
                  id="friend-username"
                  className={`${page.input} ${s.input}`}
                  type="text"
                  value={userName}
                  autoComplete="off"
                  placeholder="pseudo"
                  onChange={(event) => setUserName(event.target.value)}
                />
                <Button type="submit" variant="primary" disabled={sending || !userName.trim()}>
                  {sending ? 'Envoi…' : 'Envoyer la demande'}
                </Button>
              </div>
              {formError && <p className={s.error}>{formError}</p>}
              {formNotice && <p className={s.notice}>{formNotice}</p>}
            </form>
          </section>

          {loading && <p className={page.state}>Chargement de la liste…</p>}
          {error && <p className={s.error}>{error}</p>}
          {actionError && <p className={s.error}>{actionError}</p>}

          {!loading && !error && (
            <>
              <section className={s.section} aria-labelledby="friends-received-title">
                <SectionTitle id="friends-received-title">
                  Demandes reçues ({received.length})
                </SectionTitle>
                <Tableau
                  caption="Demandes d’amis qui attendent ta réponse"
                  columns={receivedColumns}
                  rows={received}
                  rowKey={(row) => row.idUser}
                  emptyText="Aucune demande reçue."
                />
              </section>

              <section className={s.section} aria-labelledby="friends-sent-title">
                <SectionTitle id="friends-sent-title">
                  Demandes envoyées ({sent.length})
                </SectionTitle>
                <Tableau
                  caption="Demandes d’amis en attente de réponse"
                  columns={sentColumns}
                  rows={sent}
                  rowKey={(row) => row.idUser}
                  emptyText="Aucune demande envoyée."
                />
              </section>

              <section className={s.section} aria-labelledby="friends-list-title">
                <SectionTitle id="friends-list-title">Amis ({friends.length})</SectionTitle>
                <Tableau
                  caption="Liste de tes amis"
                  columns={friendColumns}
                  rows={friends}
                  rowKey={(row) => row.idUser}
                  emptyText="Pas encore d’ami. Ajoute quelqu’un par son pseudo."
                />
              </section>

              {blocked.length > 0 && (
                <section className={s.section} aria-labelledby="friends-blocked-title">
                  <SectionTitle id="friends-blocked-title">Bloqués ({blocked.length})</SectionTitle>
                  <Tableau
                    caption="Comptes que tu as bloqués"
                    columns={blockedColumns}
                    rows={blocked}
                    rowKey={(row) => row.idUser}
                    emptyText="Aucun compte bloqué."
                  />
                </section>
              )}
            </>
          )}
        </>
      )}
    </AppLayout>
  )
}