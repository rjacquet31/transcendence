import { useState } from 'react'
import type { ReactNode } from 'react'
import { Menu } from 'lucide-react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AuthStatus } from './AuthStatus.tsx'
import { Button } from './Button.tsx'
import { Footer } from './Footer.tsx'
import { Navbar } from './Navbar.tsx'
import { Sidebar } from './Sidebar.tsx'
import s from './AppLayout.module.css'

type AppLayoutProps = {
  current: string
  onNavigate: (action: string) => void
  children: ReactNode
}

export function AppLayout({ current, onNavigate, children }: AppLayoutProps) {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <div className={s.shell}>
      <Navbar
        title="ft_transcendence"
        leading={
          <span className={s.menuToggle}>
            <Button
              variant="ghost"
              ariaLabel={menuOpen ? 'Fermer le menu' : 'Ouvrir le menu'}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <Menu size={20} />
            </Button>
          </span>
        }
        action={<SessionActions />}
      />

      <div className={s.body}>
        <Sidebar
          open={menuOpen}
          current={current}
          onNavigate={(action) => {
            setMenuOpen(false)
            onNavigate(action)
          }}
        />

        <main className={s.main} aria-label="Contenu principal">
          {children}
        </main>
      </div>

      <Footer onNavigate={onNavigate} />
    </div>
  )
}

function SessionActions() {
  const { openModal, isLoggedIn, authChecked } = useAuthModal()
  if (!authChecked) return null
  if (isLoggedIn) return <AuthStatus />

  return (
    <>
      <Button variant="ghost" onClick={() => openModal('login')}>
        Se connecter
      </Button>
      <Button variant="primary" onClick={() => openModal('signup')}>
        S'inscrire
      </Button>
    </>
  )
}
