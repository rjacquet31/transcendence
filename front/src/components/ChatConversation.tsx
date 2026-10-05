import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useChat } from '../chat/chatContext.ts'
import { useSocket } from '../ws/socketContext.ts'
import { Button } from './Button.tsx'
import page from '../style/page.module.css'
import s from './ChatConversation.module.css'

function connectionLabel(status: string) {
  if (status === 'connected') return null
  if (status === 'disconnected') return 'Connexion perdue, nouvelle tentative…'
  return 'Connexion au chat…'
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? ''
    : date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}

type ChatConversationProps = {
  targetId: string
  targetName: string
  compact?: boolean
}

export function ChatConversation({ targetId, targetName, compact = false }: ChatConversationProps) {
  const { myId, threads, historyOf, loadHistory, loadOlder, send, chatError, clearChatError, markRead } =
    useChat()
  const { status, error: socketError } = useSocket()

  const [draft, setDraft] = useState('')
  const listRef = useRef<HTMLDivElement | null>(null)
  const endRef = useRef<HTMLDivElement | null>(null)
  const keepRef = useRef<number | null>(null)

  const messages = threads[targetId] ?? []
  const history = historyOf(targetId)
  const inputId = `chat-input-${compact ? 'quick' : 'page'}-${targetId}`

  useEffect(() => {
    setDraft('')
    clearChatError()
  }, [targetId, clearChatError])

  useEffect(() => {
    loadHistory(targetId)
  }, [targetId, loadHistory])

  useEffect(() => {
    markRead(targetId)
  }, [targetId, messages.length, markRead])

  useEffect(() => {
    const list = listRef.current
    if (keepRef.current !== null && list) {
      list.scrollTop = list.scrollHeight - keepRef.current
      keepRef.current = null
      return
    }
    endRef.current?.scrollIntoView({ block: 'end' })
  }, [targetId, messages.length])

  const handleLoadOlder = () => {
    const list = listRef.current
    keepRef.current = list ? list.scrollHeight - list.scrollTop : null
    loadOlder(targetId)
  }

  const handleSend = (event: FormEvent) => {
    event.preventDefault()
    const content = draft.trim()
    if (!content) return
    if (!send(targetId, content)) return
    setDraft('')
  }

  const statusLabel = connectionLabel(status)

  return (
    <div className={compact ? `${s.root} ${s.rootCompact}` : s.root}>
      <div
        ref={listRef}
        className={compact ? `${s.messages} ${s.messagesCompact}` : s.messages}
      >
        {history.hasMore && (
          <div className={s.older}>
            <button
              type="button"
              className={s.olderButton}
              onClick={handleLoadOlder}
              disabled={history.loading}
            >
              {history.loading ? 'Chargement…' : 'Messages plus anciens'}
            </button>
          </div>
        )}

        {history.error && <p className={s.error}>{history.error}</p>}

        {!history.loaded && history.loading && messages.length === 0 && (
          <p className={s.state}>Chargement de la conversation…</p>
        )}

        {history.loaded && messages.length === 0 ? (
          <p className={s.state}>Aucun message pour l’instant. Dis bonjour !</p>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={message.senderId === myId ? `${s.bubble} ${s.bubbleMine}` : s.bubble}
            >
              <p className={s.bubbleContent}>{message.content}</p>
              <span className={s.bubbleTime}>{formatTime(message.sentAt)}</span>
            </div>
          ))
        )}
        <div ref={endRef} />
      </div>

      {(chatError ?? socketError) && <p className={s.error}>{chatError ?? socketError}</p>}
      {statusLabel && <p className={s.state}>{statusLabel}</p>}

      <form className={s.form} onSubmit={handleSend}>
        <label className="sr-only" htmlFor={inputId}>
          Message pour {targetName}
        </label>
        <input
          id={inputId}
          className={`${page.input} ${s.input}`}
          type="text"
          value={draft}
          maxLength={1000}
          autoComplete="off"
          placeholder={`Écrire à ${targetName}…`}
          onChange={(event) => setDraft(event.target.value)}
          disabled={status !== 'connected'}
        />
        <Button type="submit" variant="primary" disabled={status !== 'connected' || !draft.trim()}>
          Envoyer
        </Button>
      </form>
    </div>
  )
}