import { useEffect, useState } from 'react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { ConfirmModal } from '../components/ConfirmModal.tsx'
import { EditUserModal } from '../components/EditUserModal.tsx'
import { MenuActions } from '../components/MenuActions.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { Tableau, type Colonne } from '../components/Tableau.tsx'
import {
  deleteUser,
  getAllUsers,
  updateUser,
  type UpdateUserPayload,
  type UserPublicProfileDto,
} from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import { formatDateTime } from '../lib/format.ts'
import page from '../style/page.module.css'

type AdminPageProps = {
  onNavigate: (action: string) => void
}

export function AdminPage({ onNavigate }: AdminPageProps) {
  const { user } = useAuthModal()
  const isAdmin = user?.role === 'ADMIN'

  const [users, setUsers] = useState<UserPublicProfileDto[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<UserPublicProfileDto | null>(null)
  const [deleting, setDeleting] = useState<UserPublicProfileDto | null>(null)
  const [pending, setPending] = useState(false)
  const [modalError, setModalError] = useState<string | null>(null)

  useEffect(() => {
    if (!isAdmin) return

    let alive = true
    setLoading(true)
    setError(null)

    getAllUsers()
      .then((list) => {
        if (alive) setUsers(list)
      })
      .catch((err: unknown) => {
        if (alive) setError(errorMessage(err, 'Chargement des comptes impossible.'))
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [isAdmin])

  const askEdit = (row: UserPublicProfileDto) => {
    setModalError(null)
    setEditing(row)
  }

  const askDelete = (row: UserPublicProfileDto) => {
    setModalError(null)
    setDeleting(row)
  }

  const saveUser = async (idUser: string, values: UpdateUserPayload) => {
    setPending(true)
    setModalError(null)
    try {
      const updated = await updateUser(idUser, values)
      setUsers((list) => list.map((row) => (row.id === updated.id ? updated : row)))
      setEditing(null)
    } catch (err) {
      setModalError(errorMessage(err, 'Mise à jour impossible.'))
    } finally {
      setPending(false)
    }
  }

  const confirmDelete = async () => {
    if (!deleting) return
    const idUser = deleting.id

    setPending(true)
    setModalError(null)
    try {
      await deleteUser(idUser)
      setUsers((list) => list.filter((row) => row.id !== idUser))
      setDeleting(null)
    } catch (err) {
      setModalError(errorMessage(err, 'Suppression impossible.'))
    } finally {
      setPending(false)
    }
  }

  const columns: Colonne<UserPublicProfileDto>[] = [
    { key: 'userName', header: 'Utilisateur', render: (u) => u.userName },
    { key: 'email', header: 'E-mail', render: (u) => u.email },
    { key: 'role', header: 'Rôle', render: (u) => u.role ?? '—' },
    { key: 'online', header: 'Vu à', render: (u) => formatDateTime(u.online) },
    { key: 'id', header: 'Identifiant', render: (u) => u.id },
    {
      key: 'actions',
      header: 'Actions',
      render: (u) => (
        <MenuActions
          ariaLabel={`Actions sur ${u.userName}`}
          items={[
            { key: 'edit', label: 'Edit', onSelect: () => askEdit(u) },
            { key: 'delete', label: 'Delete', onSelect: () => askDelete(u) },
          ]}
        />
      ),
    },
  ]

  return (
    <AppLayout current="nav:admin" onNavigate={onNavigate}>
      <PageHeader
        title="Administration"
        subtitle="Gestion des comptes, des rôles et de la modération de la plateforme."
      />

      {!isAdmin ? (
        <p className={page.denied}>
          Cette page est réservée aux administrateurs. Si tu penses que c'est une erreur,
          reconnecte-toi.
        </p>
      ) : (
        <section aria-labelledby="admin-users-title">
          <SectionTitle id="admin-users-title">Utilisateurs</SectionTitle>

          {loading && <p className={page.state}>Chargement des comptes…</p>}
          {error && <p className={page.state}>{error}</p>}

          {!loading && !error && (
            <Tableau
              caption="Liste de tous les comptes enregistrés"
              columns={columns}
              rows={users}
              rowKey={(u) => u.id}
              emptyText="Aucun compte enregistré."
            />
          )}

          <EditUserModal
            user={editing}
            pending={pending}
            error={modalError}
            onClose={() => setEditing(null)}
            onSave={saveUser}
          />

          <ConfirmModal
            open={deleting !== null}
            title="Supprimer le compte"
            confirmLabel="Supprimer"
            pendingLabel="Suppression…"
            pending={pending}
            error={modalError}
            onConfirm={confirmDelete}
            onCancel={() => setDeleting(null)}
          >
            Le compte <strong>{deleting?.userName}</strong> ({deleting?.email}) sera supprimé,
            ainsi que son rôle, ses sessions et ses liens d'amitié. C'est irréversible.
          </ConfirmModal>
        </section>
      )}
    </AppLayout>
  )
}
