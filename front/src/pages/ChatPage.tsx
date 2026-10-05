import { useNavigate, useParams } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { useChat } from '../chat/chatContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { ChatConversation } from '../components/ChatConversation.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import page from '../style/page.module.css'
import s from './ChatPage.module.css'

const RECENT_LIMIT = 5

type ChatPageProps = {
  onNavigate: (action: string) => void
}

export function ChatPage({ onNavigate }: ChatPageProps) {
  const { isLoggedIn, authChecked } = useAuthModal()
  const { friends, loading, error, threads, unread, recentIds, nameOf, openQuickChat } = useChat()
  const { idUser } = useParams()
  const navigate = useNavigate()

  /* L'URL fait foi : /chat et /chat/:idUser sont deux routes, passer de l'une
     à l'autre remonte la page. */
  const selectedId = idUser ?? null

  const select = (id: string) => navigate(`/chat/${id}`)

  const recent = recentIds.slice(0, RECENT_LIMIT)
  const selectedFriend = friends.find((friend) => friend.idUser === selectedId) ?? null
  const selectedName = selectedId ? nameOf(selectedId) : null

  const previewOf = (id: string) => {
    const messages = threads[id] ?? []
    return messages.length > 0 ? messages[messages.length - 1].content : ''
  }

  const renderRow = (id: string, name: string, withPreview: boolean) => (
    <li key={id} className={s.row}>
      <button
        type="button"
        className={id === selectedId ? `${s.friendButton} ${s.friendButtonActive}` : s.friendButton}
        onClick={() => select(id)}
      >
        <span className={s.rowTop}>
          <span className={s.rowName}>{name}</span>
          {(unread[id] ?? 0) > 0 && <span className={s.badge}>{unread[id]}</span>}
        </span>
        {withPreview && previewOf(id) && <span className={s.preview}>{previewOf(id)}</span>}
      </button>

      <button
        type="button"
        className={s.popButton}
        onClick={() => openQuickChat(id)}
        aria-label={`Ouvrir un quick chat avec ${name}`}
      >
        ⧉
      </button>
    </li>
  )

  return (
    <AppLayout current="nav:chat" onNavigate={onNavigate}>
      <PageHeader title="Messages" subtitle="Discute en direct avec tes amis." />

      {!authChecked ? null : !isLoggedIn ? (
        <p className={page.denied}>Connecte-toi pour accéder à la messagerie.</p>
      ) : (
        <div className={s.layout}>
          <aside className={s.sidebar}>
            <section aria-labelledby="chat-recent-title">
              <SectionTitle id="chat-recent-title">Quick chat</SectionTitle>
              <p className={s.hint}>Tes {RECENT_LIMIT} dernières conversations.</p>

              {recent.length === 0 ? (
                <p className={s.state}>Aucun échange pour l’instant.</p>
              ) : (
                <ul className={s.friendList}>
                  {recent.map((id) => renderRow(id, nameOf(id), true))}
                </ul>
              )}
            </section>

            <section aria-labelledby="chat-friends-title">
              <SectionTitle id="chat-friends-title">Amis ({friends.length})</SectionTitle>

              {loading && <p className={s.state}>Chargement…</p>}
              {error && <p className={s.error}>{error}</p>}

              {!loading && !error && friends.length === 0 && (
                <p className={s.state}>Ajoute des amis pour pouvoir discuter.</p>
              )}

              <ul className={s.friendList}>
                {friends.map((friend) => renderRow(friend.idUser, friend.userName, false))}
              </ul>
            </section>
          </aside>

          <section className={s.conversation} aria-label="Conversation">
            {!selectedId ? (
              <p className={s.state}>Choisis un ami dans la liste pour démarrer la discussion.</p>
            ) : (
              <>
                <header className={s.conversationHeader}>
                  <h2 className={s.conversationTitle}>{selectedName}</h2>
                  <button
                    type="button"
                    className={s.popButton}
                    onClick={() => openQuickChat(selectedId)}
                    aria-label={`Ouvrir un quick chat avec ${selectedName}`}
                  >
                    ⧉
                  </button>
                </header>

                {!selectedFriend && (
                  <p className={s.state}>Cette personne ne fait plus partie de tes amis.</p>
                )}

                <ChatConversation targetId={selectedId} targetName={selectedName ?? ''} />
              </>
            )}
          </section>
        </div>
      )}
    </AppLayout>
  )
}