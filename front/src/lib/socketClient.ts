import { Client, type IFrame } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/** Handshake SockJS ; en dev, vite.config.ts le proxyfie vers le backend (sinon la connexion part vers 5173). */
const WS_ENDPOINT = '/ws'

const RECONNECT_DELAY = 3000

type StompClientHandlers = {
  /** Seul endroit où souscrire : la socket n'est pas ouverte au retour d'`activate()`. */
  onConnect: () => void
  onClose: () => void
  onError: (message: string) => void
}

/** Fabrique le transport sans souscription (le SocketProvider rejoue le registre à chaque `onConnect`), auth par cookie same-origin. */
export function createStompClient(handlers: StompClientHandlers): Client {
  const client = new Client({
    webSocketFactory: () => new SockJS(WS_ENDPOINT) as unknown as WebSocket,
    reconnectDelay: RECONNECT_DELAY,
    onConnect: handlers.onConnect,
    onWebSocketClose: handlers.onClose,
    onStompError: (frame: IFrame) => {
      handlers.onError(frame.headers.message ?? 'Erreur de connexion au serveur.')
    },
  })

  client.activate()
  return client
}
