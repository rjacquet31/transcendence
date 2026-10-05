import { useEffect, useState } from 'react'
import { X } from 'lucide-react'
import { Button } from './Button.tsx'
import { useDialog } from './useDialog.ts'
import { getFriendList, type FriendListDto } from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import type { ChessSide } from '../lib/chessClient.ts'
import { PRESENCE, type PresenceDto } from '../lib/presenceClient.ts'
import { useSocket } from '../ws/socketContext.ts'
import s from './ChessInviteModal.module.css'

type ChessInviteModalProps = {
  open: boolean
  /** Pseudo dont l'invitation part : verrouille la liste le temps de la requête (anti-doublons). */
  pending?: string | null
  /** Refus du back ; la modale reste ouverte pour que le message soit lisible. */
  error?: string | null
  onClose: () => void
  /** `side` à `null` : le back tire la couleur au sort. */
  onInvite: (userName: string, side: ChessSide | null) => void
}

/** Un bouton par ami, liste rechargée à chaque ouverture pour voir les amitiés acceptées entre-temps. */
export function ChessInviteModal({
  open,
  pending = null,
  error,
  onClose,
  onInvite,
}: ChessInviteModalProps) {
  const dialog = useDialog(open, 'button', onClose)
  const { subscribe } = useSocket()

  const [side, setSide] = useState<ChessSide | null>('WHITE')
  const [friends, setFriends] = useState<FriendListDto[]>([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)

  // Seuls les ACCEPTED sont invitables.
  useEffect(() => {
    if (!open) return

    let alive = true
    setLoading(true)
    setLoadError(null)

    getFriendList()
      .then((list) => {
        if (alive) setFriends(list.filter((row) => row.status === 'ACCEPTED'))
      })
      .catch((err: unknown) => {
        if (alive) setLoadError(errorMessage(err, 'Chargement de la liste d’amis impossible.'))
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [open])

  // Le back refuse d'inviter un absent : la liste suit la présence en direct tant que la modale est ouverte.
  useEffect(() => {
    if (!open) return
    return subscribe(PRESENCE, (body) => {
      const presence = JSON.parse(body) as PresenceDto
      setFriends((previous) =>
        previous.map((friend) =>
          friend.idUser === presence.idUser ? { ...friend, online: presence.online } : friend,
        ),
      )
    })
  }, [open, subscribe])

  // En ligne d'abord, puis par pseudo.
  const sorted = [...friends].sort(
    (a, b) => Number(b.online) - Number(a.online) || a.userName.localeCompare(b.userName),
  )

  return (
    <dialog {...dialog} className={s.dialog} aria-labelledby="chess-invite-title">
      <div className={s.head}>
        <h2 id="chess-invite-title" className={s.title}>
          Inviter un(e) ami(e)
        </h2>
        <Button variant="ghost" ariaLabel="Fermer" onClick={onClose}>
          <X size={18} />
        </Button>
      </div>

      {/* Le camp se choisit avant l'ami : l'invité recevra l'autre couleur. */}
      <div className={s.field}>
        <span className={s.label} id="chess-invite-side">
          Votre camp
        </span>
        <div className={s.sides} role="group" aria-labelledby="chess-invite-side">
          <Button
            variant={side === 'WHITE' ? 'primary' : 'secondary'}
            fullWidth
            disabled={pending !== null}
            onClick={() => setSide('WHITE')}
          >
            Blancs
          </Button>
          <Button
            variant={side === 'BLACK' ? 'primary' : 'secondary'}
            fullWidth
            disabled={pending !== null}
            onClick={() => setSide('BLACK')}
          >
            Noirs
          </Button>
          <Button
            variant={side === null ? 'primary' : 'secondary'}
            fullWidth
            disabled={pending !== null}
            onClick={() => setSide(null)}
          >
            Aléatoire
          </Button>
        </div>
      </div>

      {loading && <p className={s.status}>Chargement…</p>}

      {!loading && loadError && (
        <p className={s.globalError} role="alert">
          {loadError}
        </p>
      )}

      {!loading && !loadError && friends.length === 0 && (
        <p className={s.status}>Aucun ami à inviter pour l’instant.</p>
      )}

      {!loading && !loadError && friends.length > 0 && (
        <ul className={s.friendList}>
          {sorted.map((friend) => (
            <li key={friend.idUser} className={friend.online ? undefined : s.offline}>
              <Button
                variant="secondary"
                fullWidth
                disabled={pending !== null || !friend.online}
                ariaLabel={friend.online ? undefined : `${friend.userName}, hors ligne`}
                onClick={() => onInvite(friend.userName, side)}
              >
                {pending === friend.userName ? 'Envoi…' : friend.userName}
                {!friend.online && <span className={s.offlineTag}>hors ligne</span>}
              </Button>
            </li>
          ))}
        </ul>
      )}

      {error && (
        <p className={s.globalError} role="alert">
          {error}
        </p>
      )}
    </dialog>
  )
}
