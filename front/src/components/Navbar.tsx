import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from './Button.tsx'
import { useAuthModal } from '../auth/authModalContext.ts'
import { NotificationBell } from './NotificationBell.tsx'
import s from './Navbar.module.css'

type NavbarProps = {
  title: string
  /** Slot de gauche (ex: bouton menu en mobile). */
  leading?: ReactNode
  /** Slot de droite (ex: Se connecter / S'inscrire). */
  action?: ReactNode
}

export function Navbar({ title, leading, action }: NavbarProps) {
  const { isLoggedIn, logout } = useAuthModal()
  const navigate = useNavigate()

  return (
    <nav className={s.navbar}>
      <div className={s.side}>{leading}</div>
      <button type="button" className={s.title} onClick={() => navigate('/')}>
        {title}
      </button>
      <div className={`${s.side} ${s.sideEnd}`}>
        {action}
        {isLoggedIn && <NotificationBell />}
        {isLoggedIn && (
          <Button variant="ghost" onClick={() => void logout()}>
            Déconnexion
          </Button>
        )}
      </div>
    </nav>
  )
}
