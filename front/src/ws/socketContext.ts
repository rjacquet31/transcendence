import { createContext, useContext } from 'react'

export type SocketStatus = 'idle' | 'connecting' | 'connected' | 'disconnected'

export type SocketValue = {
  status: SocketStatus
  error: string | null
  subscribe: (destination: string, handler: (body: string) => void) => () => void
  publish: (destination: string, body: unknown) => boolean
}
export const SocketContext = createContext<SocketValue | null>(null)

export function useSocket() {
  const ctx = useContext(SocketContext)
  if (!ctx) throw new Error('useSocket doit être utilisé dans SocketProvider')
  return ctx
}
