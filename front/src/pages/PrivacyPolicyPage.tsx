import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import s from './LegalPage.module.css'

type PrivacyPolicyPageProps = {
  onNavigate: (action: string) => void
}

export function PrivacyPolicyPage({ onNavigate }: PrivacyPolicyPageProps) {
  return (
    <AppLayout current="nav:privacy" onNavigate={onNavigate}>
      <PageHeader
        title="Politique de confidentialité"
        subtitle="Ce que ce service enregistre sur toi, pourquoi, et ce que tu peux exiger."
      />

      <article className={s.doc}>
        <p className={s.updated}>Dernière mise à jour : 15 septembre 2026</p>

        <section className={s.block}>
          <h2 className={s.heading}>1. Qui gère ce service</h2>
          <p className={s.text}>
            ft_transcendence est un projet pédagogique réalisé par des étudiants de l’École 42. Il
            n’a aucune finalité commerciale : pas de publicité, pas de revente de données, pas de
            profilage. L’équipe du projet est responsable du traitement au sens du RGPD.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>2. Données que nous enregistrons</h2>

          <h3 className={s.subheading}>Compte</h3>
          <ul className={s.list}>
            <li>Pseudo et adresse e-mail.</li>
            <li>Mot de passe, stocké uniquement sous forme hachée — jamais en clair.</li>
            <li>Rôle du compte et date de création.</li>
          </ul>

          <h3 className={s.subheading}>Profil</h3>
          <ul className={s.list}>
            <li>Avatar, si tu en envoies un.</li>
            <li>Date de dernière connexion, qui sert à afficher ton statut en ligne.</li>
          </ul>

          <h3 className={s.subheading}>Relations</h3>
          <ul className={s.list}>
            <li>Demandes d’amis envoyées et reçues, avec leur date et leur statut.</li>
            <li>Liste d’amis et liste des comptes que tu as bloqués.</li>
          </ul>

          <h3 className={s.subheading}>Messages</h3>
          <ul className={s.list}>
            <li>Contenu des messages privés, expéditeur, destinataire, horodatage, statut de lecture.</li>
            <li>
              Les messages sont stockés en clair côté serveur : ils ne sont pas chiffrés de bout en
              bout. N’y écris rien que tu ne confierais pas à l’équipe qui administre la base.
            </li>
          </ul>

          <h3 className={s.subheading}>Jeu</h3>
          <ul className={s.list}>
            <li>Invitations, parties d’échecs, coups joués, résultats et abandons.</li>
          </ul>

          <h3 className={s.subheading}>Technique</h3>
          <ul className={s.list}>
            <li>Un cookie de session, strictement nécessaire pour te garder connecté.</li>
            <li>Journaux serveur techniques (erreurs, accès), conservés pour le débogage.</li>
          </ul>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>3. Pourquoi, et sur quelle base</h2>
          <ul className={s.list}>
            <li>
              <span className={s.strong}>Fournir le service</span> — compte, chat, amis, parties.
              Base légale : l’exécution du service que tu as demandé en créant un compte.
            </li>
            <li>
              <span className={s.strong}>Sécurité et bon fonctionnement</span> — authentification,
              blocage d’un compte abusif, correction de bugs. Base légale : intérêt légitime.
            </li>
          </ul>
          <p className={s.text}>
            Aucune donnée n’est utilisée pour de la publicité, du ciblage ou une décision
            automatisée.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>4. Qui peut voir tes données</h2>
          <ul className={s.list}>
            <li>
              <span className={s.strong}>Les autres joueurs</span> voient ton pseudo, ton avatar,
              ton statut en ligne, les messages que tu leur envoies et les parties jouées contre
              eux.
            </li>
            <li>
              <span className={s.strong}>Les administrateurs</span> du service peuvent consulter et
              modifier les comptes dans l’interface d’administration.
            </li>
            <li>
              <span className={s.strong}>Personne d’autre.</span> Aucune donnée n’est transmise à un
              tiers, ni vendue, ni hébergée hors de l’infrastructure du projet.
            </li>
          </ul>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>5. Combien de temps</h2>
          <ul className={s.list}>
            <li>Tant que ton compte existe, tes données restent enregistrées.</li>
            <li>
              Si tu supprimes ton compte, profil, avatar, relations et messages sont effacés. Les
              parties d’échecs sont conservées sous une identité anonyme, pour ne pas amputer
              l’historique de tes adversaires.
            </li>
            <li>
              Si tu anonymises ton compte, pseudo, e-mail et avatar sont remplacés ou effacés, et
              plus rien ne relie l’activité restante à ton identité.
            </li>
            <li>S’agissant d’un projet d’étude, la base peut être réinitialisée entre deux évaluations.</li>
          </ul>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>6. Tes droits</h2>
          <ul className={s.list}>
            <li>
              <span className={s.strong}>Accès et portabilité</span> — télécharge l’intégralité de
              tes données en JSON depuis la page Paramètres.
            </li>
            <li>
              <span className={s.strong}>Rectification</span> — modifie ton pseudo, ton e-mail et
              ton avatar depuis ta page Profil.
            </li>
            <li>
              <span className={s.strong}>Effacement</span> — supprime ton compte depuis la page
              Paramètres, sans avoir à te justifier.
            </li>
            <li>
              <span className={s.strong}>Anonymisation</span> — conserve ton compte en retirant
              toute donnée identifiante, depuis la page Paramètres.
            </li>
          </ul>
          <p className={s.text}>
            Ces droits s’exercent directement dans l’application, sans demande préalable ni délai de
            traitement. Tu peux aussi introduire une réclamation auprès de la CNIL.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>7. Cookies</h2>
          <p className={s.text}>
            Le service pose un seul cookie, celui de ta session d’authentification. Il est
            strictement nécessaire au fonctionnement et disparaît à la déconnexion. Aucun traceur,
            aucun cookie publicitaire, aucun outil de mesure d’audience tiers.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>8. Sécurité</h2>
          <p className={s.text}>
            Les mots de passe sont hachés, les échanges passent par HTTPS et l’accès à la base est
            restreint à l’équipe du projet. Aucun système n’est infaillible : ne réutilise pas ici un
            mot de passe qui protège un compte important ailleurs.
          </p>
        </section>

        <section className={s.block}>
          <h2 className={s.heading}>9. Modifications et contact</h2>
          <p className={s.text}>
            Cette politique peut évoluer avec le projet ; la date en haut de page indique la
            dernière version. Pour toute question sur tes données, contacte l’équipe du projet à
            l’adresse indiquée dans le dépôt.
          </p>
        </section>

        <div className={s.footerNav}>
          <Button onClick={() => onNavigate('nav:terms')}>Conditions d’utilisation</Button>
          <Button variant="ghost" onClick={() => onNavigate('nav:settings')}>
            Retour aux paramètres
          </Button>
        </div>
      </article>
    </AppLayout>
  )
}