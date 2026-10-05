import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import s from './LegalPage.module.css'

type TermsPageProps = {
  onNavigate: (action: string) => void
}

export function TermsPage({ onNavigate }: TermsPageProps) {
  return (
    <AppLayout current="nav:terms" onNavigate={onNavigate}>
      <PageHeader
        title="Conditions d’utilisation"
        subtitle="Les règles du service, ce qui est attendu de toi et ce que tu peux en attendre."
      />

      <article className={s.doc}>
        <p className={s.updated}>Dernière mise à jour : 15 septembre 2026</p>

        <section className={s.block}>
          <h2 className={s.heading}>1. Objet</h2>
          <p className={s.text}>
            ft_transcendence est une plateforme de jeu et de discussion développée dans le cadre du
            cursus de l’École 42. Créer un compte vaut acceptation des présentes conditions. Si tu
            n’en acceptes pas les termes, n’utilise pas le service.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>2. Ton compte</h2>
          <ul className={s.list}>
            <li>Un compte est personnel : ne le partage pas et ne te connecte pas à celui d’un autre.</li>
            <li>Fournis une adresse e-mail valide et un pseudo qui n’usurpe l’identité de personne.</li>
            <li>
              Tu es responsable de la confidentialité de ton mot de passe et de ce qui se passe sous
              ton compte.
            </li>
            <li>
              Le service n’est pas destiné aux enfants : si tu as moins de 15 ans, l’accord de tes
              représentants légaux est nécessaire.
            </li>
          </ul>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>3. Règles de conduite</h2>
          <p className={s.text}>Sont interdits sur la plateforme :</p>
          <ul className={s.list}>
            <li>
              le harcèlement, les menaces, les insultes, les propos haineux ou discriminatoires ;
            </li>
            <li>
              les contenus illicites, sexuellement explicites, ou mettant en cause la vie privée
              d’autrui ;
            </li>
            <li>le spam, la publicité et les tentatives d’hameçonnage dans la messagerie ;</li>
            <li>
              l’usage d’un moteur d’échecs ou de tout automate pour jouer à ta place contre un
              humain ;
            </li>
            <li>
              toute tentative d’atteinte à l’infrastructure : injection, contournement
              d’authentification, surcharge délibérée, exploitation d’une faille sans la signaler.
            </li>
          </ul>
          <p className={s.text}>
            Si tu découvres une vulnérabilité, signale-la à l’équipe plutôt que de l’exploiter.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>4. Ce que tu publies</h2>
          <p className={s.text}>
            Tu restes responsable de tes messages, de ton pseudo et de ton avatar. En les publiant,
            tu autorises le service à les stocker et à les afficher aux personnes concernées, pour
            le seul fonctionnement de la plateforme. N’envoie pas de contenu dont tu ne détiens pas
            les droits.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>5. Modération</h2>
          <ul className={s.list}>
            <li>
              Tu disposes toi-même d’outils : tu peux refuser une demande d’ami, retirer un ami ou
              bloquer un compte à tout moment.
            </li>
            <li>
              Les administrateurs peuvent modifier ou supprimer un compte qui enfreint ces règles,
              sans préavis en cas de manquement grave.
            </li>
          </ul>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>6. Disponibilité du service</h2>
          <p className={s.text}>
            Le service est fourni en l’état, dans le cadre d’un projet étudiant. Aucune
            disponibilité n’est garantie : il peut être interrompu, modifié ou remis à zéro sans
            préavis, notamment entre deux évaluations. Exporte tes données depuis la page Paramètres
            si tu tiens à les conserver.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>7. Responsabilité</h2>
          <p className={s.text}>
            Dans les limites permises par la loi, l’équipe du projet ne peut être tenue responsable
            des pertes de données, des interruptions de service, ni des contenus publiés par les
            utilisateurs. Elle reste responsable de ses propres manquements en matière de protection
            des données.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>8. Fin de l’utilisation</h2>
          <p className={s.text}>
            Tu peux supprimer ou anonymiser ton compte à tout moment depuis la page Paramètres,
            sans justification. Les conséquences de chaque opération sont détaillées dans la{' '}
            politique de confidentialité.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>9. Droit applicable et évolutions</h2>
          <p className={s.text}>
            Ces conditions relèvent du droit français. Elles peuvent être modifiées avec le projet ;
            la date en haut de page indique la version en vigueur. Continuer à utiliser le service
            après une modification vaut acceptation de la nouvelle version.
          </p>
        </section>

        <div className={s.footerNav}>
          <Button onClick={() => onNavigate('nav:privacy')}>Politique de confidentialité</Button>
          <Button variant="ghost" onClick={() => onNavigate('nav:settings')}>
            Retour aux paramètres
          </Button>
        </div>
      </article>
    </AppLayout>
  )
}