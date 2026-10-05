import { createContext, useContext } from 'react'
import type { UserPublicProfileDto } from '../lib/actions.ts'

export type AuthMode = 'login' | 'signup' | 'forgot'

export type AuthModalValue = {
  isOpen: boolean
  mode: AuthMode
  isLoggedIn: boolean
  user: UserPublicProfileDto | null
  /** `false` tant que la session n'a pas été vérifiée au chargement de la page. */
  authChecked: boolean
  /** Ouvre la modale sur l'onglet demandé (connexion par défaut). */
  openModal: (mode?: AuthMode) => void
  closeModal: () => void
  logout: () => Promise<void>
  /** Relit `/users/me` et resynchronise la session : la navbar et la sidebar
   *  suivent un changement de pseudo, d'e-mail ou d'avatar. */
  refreshUser: () => Promise<void>
}

/** Fichier sans composant : le hook et le contexte peuvent être exportés ensemble. */
export const AuthModalContext = createContext<AuthModalValue | null>(null)

export function useAuthModal() {
  const ctx = useContext(AuthModalContext)
  if (!ctx) throw new Error('useAuthModal doit être utilisé dans AuthModalProvider')
  return ctx
}
