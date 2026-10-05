import { useState, type FormEvent } from 'react'
import { X } from 'lucide-react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { ConfirmModal } from '../components/ConfirmModal.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { UndercoverChat } from '../components/UndercoverChat.tsx'
import { UndercoverRoomPanel } from '../components/UndercoverRoomPanel.tsx'
import { UndercoverTable } from '../components/UndercoverTable.tsx'
import { useUndercover } from '../ws/useUndercover.ts'
import s from './UndercoverPage.module.css'

type UndercoverPageProps = {
  onNavigate: (action: string) => void
}

/** Trois écrans selon ce que le serveur dit du compte : une partie en cours
 *  prime sur une salle, et sans l'un ni l'autre on est au lobby. */
export function UndercoverPage({ onNavigate }: UndercoverPageProps) {
  const { user, isLoggedIn, authChecked } = useAuthModal()
  const undercover = useUndercover()
  const { room, match, stats, loading, pending, error } = undercover

  const [code, setCode] = useState('')
  const [confirmLeave, setConfirmLeave] = useState(false)

  const join = async (event: FormEvent) => {
    event.preventDefault()
    if (await undercover.joinRoom(code)) setCode('')
  }

  const leaveMatch = async () => {
    if (await undercover.leaveMatch()) setConfirmLeave(false)
  }

  const subtitle =
    match !== null
      ? 'Donnez des indices sans vous trahir, et démasquez les imposteurs.'
      : room !== null
        ? 'La salle se remplit : partagez le code, puis l’hôte lance la partie.'
        : 'Un mot pour tous, un autre pour l’undercover. Qui ment ?'

  return (
    <AppLayout current="nav:undercover" onNavigate={onNavigate}>
      <PageHeader title="Undercover" subtitle={subtitle} />

      {error !== null && (
        <p className={s.error} role="alert">
          {error}
          <Button variant="ghost" ariaLabel="Fermer" onClick={undercover.clearError}>
            <X size={18} />
          </Button>
        </p>
      )}

      {authChecked && !isLoggedIn ? (
          <p className={s.lead}>Connecte-toi pour jouer à Undercover.</p>
      ) : loading ? (
        <p className={s.hint}>Chargement…</p>
      ) : match !== null ? (
        <div className={s.matchLayout}>
          <UndercoverTable
            match={match}
            pending={pending}
            onClue={undercover.clue}
            onVote={(seat) => void undercover.vote(seat)}
            onGuess={undercover.guess}
            onLeave={() => setConfirmLeave(true)}
            onClose={undercover.closeFinished}
          />
          <UndercoverChat
            messages={undercover.chat}
            mySeat={match.mySeat}
            canWrite={match.status === 'IN_PROGRESS' && !match.iAmEliminated}
            pending={pending}
            onSend={undercover.sendChat}
          />
          <ConfirmModal
            open={confirmLeave}
            title="Quitter la partie ?"
            confirmLabel="Quitter"
            pendingLabel="Départ…"
            pending={pending}
            onConfirm={() => void leaveMatch()}
            onCancel={() => setConfirmLeave(false)}
          >
            {match.iAmEliminated
              ? 'Vous ne suivrez plus cette partie.'
              : 'Vous serez éliminé·e sur-le-champ et la partie comptera comme une défaite.'}
          </ConfirmModal>
        </div>
      ) : room !== null && user !== null ? (
        <UndercoverRoomPanel
          room={room}
          myId={user.id}
          pending={pending}
          onConfigure={undercover.configure}
          onChooseWords={undercover.chooseWords}
          onStart={() => void undercover.start()}
          onLeave={() => void undercover.leaveRoom()}
        />
      ) : (
        <div className={s.lobby}>
          <section className={s.card} aria-labelledby="uc-create">
            <SectionTitle id="uc-create">Nouvelle salle</SectionTitle>
            <p className={s.lead}>
              Créez une salle, choisissez le mode et partagez le code. Il faut au moins 3 joueurs.
            </p>
            <Button variant="primary" disabled={pending} onClick={() => void undercover.createRoom()}>
              Créer une salle
            </Button>
          </section>

          <section className={s.card} aria-labelledby="uc-join">
            <SectionTitle id="uc-join">Rejoindre</SectionTitle>
            <form className={s.joinForm} onSubmit={(event) => void join(event)}>
              <input
                className={s.codeInput}
                value={code}
                maxLength={6}
                placeholder="CODE"
                aria-label="Code de la salle"
                autoComplete="off"
                onChange={(event) => setCode(event.target.value.toUpperCase())}
              />
              <Button type="submit" variant="primary" disabled={pending || code.trim().length !== 6}>
                Rejoindre
              </Button>
            </form>
          </section>

          <section className={s.card} aria-labelledby="uc-rules">
            <SectionTitle id="uc-rules">Comment jouer</SectionTitle>
            <ol className={s.rules}>
              <li>Tout le monde reçoit le même mot, sauf l’undercover qui en reçoit un proche. Personne ne sait qui il est.</li>
              <li>Chacun son tour, donnez un indice sur votre mot — assez précis pour vos alliés, assez flou pour ne pas vous trahir.</li>
              <li>Votez pour éliminer le joueur que vous soupçonnez. Son rôle est alors révélé.</li>
              <li>Mr White n’a pas de mot : démasqué, il peut encore gagner en devinant celui des agents.</li>
            </ol>
          </section>

          {stats !== null && (
            <section className={s.card} aria-labelledby="uc-stats">
              <SectionTitle id="uc-stats">Votre bilan</SectionTitle>
              <div className={s.stats}>
                <p className={s.stat}>
                  <span className={s.statValue}>{stats.wins}</span>
                  <span className={s.statLabel}>victoires</span>
                </p>
                <p className={s.stat}>
                  <span className={s.statValue}>{stats.losses}</span>
                  <span className={s.statLabel}>défaites</span>
                </p>
              </div>
            </section>
          )}
        </div>
      )}
    </AppLayout>
  )
}
