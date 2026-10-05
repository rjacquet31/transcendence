import { useState } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate } from 'react-router-dom'
import { ChatConversation } from '../components/ChatConversation.tsx'
import { useChat } from './chatContext.ts'
import s from './QuickChatDock.module.css'

const MINIMIZED_STORAGE = 'transcendence.quickchat.minimized'

function readStoredMinimized(): Record<string, boolean> {
  try {
    return JSON.parse(sessionStorage.getItem(MINIMIZED_STORAGE) ?? '{}')
  } catch {
    return {}
  }
}

function writeStoredMinimized(state: Record<string, boolean>) {
  // setItem lève en navigation privée ou quota plein : ce n'est qu'un confort.
  try {
    sessionStorage.setItem(MINIMIZED_STORAGE, JSON.stringify(state))
  } catch {
    return
  }
}

export function QuickChatDock() {
  const { quickChatIds, closeQuickChat, nameOf, unread } = useChat()
  const [minimized, setMinimized] = useState<Record<string, boolean>>(readStoredMinimized)
  const navigate = useNavigate()

  const toggle = (idUser: string) => {
    const next = { ...minimized, [idUser]: minimized[idUser] !== true }
    setMinimized(next)
    writeStoredMinimized(next)
  }

  if (quickChatIds.length === 0) return null

  return createPortal(
    <div className={s.dock} aria-live="polite">
      {quickChatIds.map((idUser) => {
        const name = nameOf(idUser)
        const isMinimized = minimized[idUser] === true
        const count = unread[idUser] ?? 0

        return (
          <section
            key={idUser}
            className={isMinimized ? `${s.window} ${s.windowMinimized}` : s.window}
            aria-label={`Quick chat avec ${name}`}
          >
            <header className={s.header}>
              <button
                type="button"
                className={s.title}
                onClick={() => toggle(idUser)}
                aria-expanded={!isMinimized}
              >
                <span className={s.titleText}>{name}</span>
                {isMinimized && count > 0 && <span className={s.badge}>{count}</span>}
              </button>

              <div className={s.headerActions}>
                <button
                  type="button"
                  className={s.iconButton}
                  onClick={() => navigate(`/chat/${idUser}`)}
                  aria-label={`Ouvrir la conversation avec ${name} en pleine page`}
                >
                  ↗
                </button>
                <button
                  type="button"
                  className={s.iconButton}
                  onClick={() => toggle(idUser)}
                  aria-label={isMinimized ? `Agrandir ${name}` : `Réduire ${name}`}
                >
                  {isMinimized ? '▴' : '▾'}
                </button>
                <button
                  type="button"
                  className={s.iconButton}
                  onClick={() => closeQuickChat(idUser)}
                  aria-label={`Fermer le quick chat avec ${name}`}
                >
                  ×
                </button>
              </div>
            </header>

            {!isMinimized && (
              <div className={s.body}>
                <ChatConversation targetId={idUser} targetName={name} compact />
              </div>
            )}
          </section>
        )
      })}
    </div>,
    document.body,
  )
}