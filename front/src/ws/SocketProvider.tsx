import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import type { Client, StompSubscription } from '@stomp/stompjs'
import { useAuthModal } from '../auth/authModalContext.ts'
import { createStompClient } from '../lib/socketClient.ts'
import { SocketContext, type SocketStatus, type SocketValue } from './socketContext.ts'

/** Au-delà de ce nombre d'échecs consécutifs on abandonne (souvent une session expirée). */
const MAX_CONSECUTIVE_FAILURES = 5

type Registration = {
  destination: string
  handler: (body: string) => void
  /** `null` tant que la socket n'est pas ouverte : la souscription STOMP n'est posée qu'au `onConnect`. */
  sub: StompSubscription | null
}

/** Possède l'unique connexion STOMP, au-dessus des routes : elle survit à la navigation et ne ferme qu'à la déconnexion. */
export function SocketProvider({ children }: { children: ReactNode }) {
  const { authChecked, isLoggedIn, user } = useAuthModal()
  const userId = user?.id ?? null

  const [status, setStatus] = useState<SocketStatus>('idle')
  const [error, setError] = useState<string | null>(null)

  const clientRef = useRef<Client | null>(null)
  const registryRef = useRef(new Map<number, Registration>())
  const nextIdRef = useRef(0)
  const failuresRef = useRef(0)

  useEffect(() => {
    if (!authChecked || !isLoggedIn || !userId) return

    // Copié ici : la lire dans le cleanup via la ref avertirait react-hooks.
    const registry = registryRef.current

    failuresRef.current = 0
    setStatus('connecting')
    setError(null)

    const client = createStompClient({
      onConnect: () => {
        // Un client remplacé par un remontage (StrictMode, changement de compte) ne touche plus l'état partagé.
        if (clientRef.current !== client) return
        failuresRef.current = 0
        setStatus('connected')
        setError(null)
        // stompjs ne restaure pas les souscriptions à la reconnexion : on rejoue le registre.
        for (const entry of registry.values()) {
          entry.sub = client.subscribe(entry.destination, (frame) => entry.handler(frame.body))
        }
      },
      onClose: () => {
        if (clientRef.current !== client) return
        for (const entry of registry.values()) entry.sub = null
        setStatus('disconnected')
        failuresRef.current += 1
        if (failuresRef.current >= MAX_CONSECUTIVE_FAILURES) {
          setError('Connexion temps réel perdue. Recharge la page ou reconnecte-toi.')
          void client.deactivate()
        }
      },
      onError: (message) => {
        if (clientRef.current === client) setError(message)
      },
    })
    clientRef.current = client

    return () => {
      clientRef.current = null
      for (const entry of registry.values()) entry.sub = null
      setStatus('idle')
      void client.deactivate()
    }
  }, [authChecked, isLoggedIn, userId])

  /** Référence stable pour que les effets des pages ne se relancent pas à chaque rendu. */
  const subscribe = useCallback((destination: string, handler: (body: string) => void) => {
    const id = nextIdRef.current++
    const entry: Registration = { destination, handler, sub: null }
    registryRef.current.set(id, entry)

    const client = clientRef.current
    if (client?.connected) {
      entry.sub = client.subscribe(destination, (frame) => handler(frame.body))
    }

    return () => {
      registryRef.current.delete(id)
      if (clientRef.current?.connected) entry.sub?.unsubscribe()
      entry.sub = null
    }
  }, [])

  const publish = (destination: string, body: unknown) => {
    const client = clientRef.current
    if (!client?.connected) return false
    client.publish({ destination, body: JSON.stringify(body) })
    return true
  }

  const value: SocketValue = { status, error, subscribe, publish }

  return <SocketContext.Provider value={value}>{children}</SocketContext.Provider>
}
