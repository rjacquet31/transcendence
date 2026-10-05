import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Button } from './Button.tsx'
import type { UndercoverChatMessage } from '../lib/undercoverClient.ts'
import s from './UndercoverChat.module.css'

type UndercoverChatProps = {
  messages: UndercoverChatMessage[]
  mySeat: number
  /** Éliminé ou partie finie : on lit la table sans pouvoir y écrire. */
  canWrite: boolean
  pending: boolean
  onSend: (text: string) => Promise<boolean>
}

const MAX_LENGTH = 300

/** Le chat de la table, ouvert pendant toute la partie. Un message n'apparaît
 *  qu'au retour de la socket, y compris le sien : tout le monde les voit ainsi
 *  dans le même ordre. */
export function UndercoverChat({ messages, mySeat, canWrite, pending, onSend }: UndercoverChatProps) {
  const [text, setText] = useState('')
  const listRef = useRef<HTMLOListElement>(null)

  // Reste collé au dernier message à chaque arrivée.
  useEffect(() => {
    const list = listRef.current
    if (list) list.scrollTop = list.scrollHeight
  }, [messages.length])

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (await onSend(text)) setText('')
  }

  return (
    <aside className={s.panel} aria-label="Chat de la partie">
      <p className={s.blockTitle}>Discussion</p>

      <ol ref={listRef} className={s.list} aria-live="polite">
        {messages.length === 0 && <li className={s.empty}>Débattez ici avant de voter.</li>}
        {messages.map((message) => (
          <li
            key={`${message.sentAt}-${message.seat}`}
            className={message.seat === mySeat ? `${s.message} ${s.mine}` : s.message}
          >
            <span className={s.author}>{message.userName ?? 'Joueur'}</span>
            <span className={s.text}>{message.text}</span>
          </li>
        ))}
      </ol>

      {canWrite ? (
        <form className={s.form} onSubmit={(event) => void submit(event)}>
          <input
            className={s.input}
            value={text}
            maxLength={MAX_LENGTH}
            placeholder="Votre message…"
            aria-label="Votre message"
            autoComplete="off"
            onChange={(event) => setText(event.target.value)}
          />
          <Button type="submit" variant="primary" disabled={pending || text.trim() === ''}>
            Envoyer
          </Button>
        </form>
      ) : (
        <p className={s.empty}>Vous ne pouvez plus écrire dans cette partie.</p>
      )}
    </aside>
  )
}
