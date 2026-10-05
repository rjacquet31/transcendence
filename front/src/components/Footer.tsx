import { Button } from './Button.tsx'
import s from './Footer.module.css'

type FooterProps = {
  onNavigate: (action: string) => void
}

export function Footer({ onNavigate }: FooterProps) {
  return (
    <footer className={s.footer}>
      <p className={s.copy}>ft_transcendence — projet 42</p>
      <nav className={s.links} aria-label="Liens légaux">
        <Button variant="ghost" onClick={() => onNavigate('nav:privacy')}>
          Politique de confidentialité
        </Button>
        <Button variant="ghost" onClick={() => onNavigate('nav:terms')}>
          Conditions d'utilisation
        </Button>
      </nav>
    </footer>
  )
}
