import { Crown, VenetianMask } from 'lucide-react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { GameCard } from '../components/GameCard.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import s from './HomePage.module.css'

const GAMES = [
  {
    action: 'nav:chess',
    name: 'Échecs',
    icon: Crown,
    description: 'Le grand classique. Parties classées, historique complet de tes coups.',
  },
  {
    action: 'nav:undercover',
    name: 'Undercover',
    icon: VenetianMask,
    description: 'Un mot pour tous, un autre pour l’undercover. Donne tes indices sans te trahir.',
  },
]

type HomePageProps = {
  onNavigate: (action: string) => void
}

export function HomePage({ onNavigate }: HomePageProps) {
  const { isLoggedIn, user, authChecked } = useAuthModal()

  const title =
    authChecked && isLoggedIn && user ? `Bienvenue ${user.userName}` : 'Bienvenue sur le hub'

  return (
    <AppLayout current="nav:home" onNavigate={onNavigate}>
      <PageHeader
        className={s.heroDecor}
        title={title}
        subtitle="Échecs contre des bots ou en ligne, Undercover entre amis."
      />

      <section aria-labelledby="games-title">
        <SectionTitle id="games-title">Les jeux</SectionTitle>
        <div className={s.gameGrid}>
          {GAMES.map(({ action, ...game }) => (
            <GameCard key={action} {...game} onPlay={() => onNavigate(action)} />
          ))}
        </div>
      </section>
    </AppLayout>
  )
}
