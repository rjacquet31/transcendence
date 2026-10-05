import { useEffect, useState, type FormEvent } from 'react'
import { Button } from './Button.tsx'
import type { UndercoverMatchView, UndercoverPlayer } from '../lib/undercoverClient.ts'
import { PHASE_LABELS, RESULT_LABELS, ROLE_LABELS } from '../lib/undercoverLabels.ts'
import s from './UndercoverTable.module.css'

type UndercoverTableProps = {
  match: UndercoverMatchView
  pending: boolean
  onClue: (word: string) => Promise<boolean>
  onVote: (seat: number) => void
  onGuess: (word: string) => Promise<boolean>
  onLeave: () => void
  onClose: () => void
}

/** Secondes restantes avant `deadline`, rafraîchies deux fois par seconde.
 *
 *  L'écart entre l'horloge locale et celle du serveur est mesuré à chaque
 *  nouvelle vue (`serverTime`) : un poste en avance de dix secondes afficherait
 *  sinon un chrono qui tombe à zéro bien avant le serveur.
 *
 *  L'état n'est écrit que depuis le minuteur, jamais pendant l'effet lui-même. */
function useCountdown(deadline: string | null, serverTime: string) {
  const [remaining, setRemaining] = useState<number | null>(null)

  useEffect(() => {
    if (deadline === null) return
    const offset = new Date(serverTime).getTime() - Date.now()
    const end = new Date(deadline).getTime()
    const tick = () => setRemaining(Math.max(0, Math.ceil((end - (Date.now() + offset)) / 1000)))

    const first = setTimeout(tick, 0)
    const timer = setInterval(tick, 500)
    return () => {
      clearTimeout(first)
      clearInterval(timer)
    }
  }, [deadline, serverTime])

  return deadline === null ? null : remaining
}

/** Mot secret masqué par défaut, révélé d'un clic, pour jouer sans qu'un
 *  voisin lise l'écran.
 *
 *  Masqué, on affiche un nombre fixe de points : un flou laisserait deviner la
 *  longueur du mot. */
function SecretWord({ word }: { word: string }) {
  const [hidden, setHidden] = useState(true)

  return (
    <button
      type="button"
      className={hidden ? `${s.secretWord} ${s.secretHidden}` : s.secretWord}
      aria-pressed={hidden}
      title={hidden ? 'Afficher le mot' : 'Cacher le mot'}
      onClick={() => setHidden((value) => !value)}
    >
      {hidden ? '••••••' : word}
    </button>
  )
}

function nameOf(player: UndercoverPlayer | undefined) {
  return player?.userName ?? 'Joueur'
}

export function UndercoverTable({
  match,
  pending,
  onClue,
  onVote,
  onGuess,
  onLeave,
  onClose,
}: UndercoverTableProps) {
  const remaining = useCountdown(match.phaseDeadline, match.serverTime)

  const bySeat = new Map(match.players.map((player) => [player.seat, player]))
  const me = bySeat.get(match.mySeat)
  const over = match.status !== 'IN_PROGRESS'
  const alive = match.players.filter((player) => !player.eliminated)

  const myTurn =
    match.phase === 'DESCRIPTION' && !match.iAmEliminated && match.currentSpeakerSeat === match.mySeat
  const iVoted = match.votedSeats.includes(match.mySeat)
  const canVote = match.phase === 'VOTE' && !match.iAmEliminated && !iVoted
  const iGuess = match.phase === 'MR_WHITE_GUESS' && match.guesserSeat === match.mySeat
  const revote = match.phase === 'VOTE' && match.voteAttempt > 0

  const isVotable = (player: UndercoverPlayer) =>
    canVote &&
    !player.eliminated &&
    player.seat !== match.mySeat &&
    (!revote || match.tiedSeats.includes(player.seat))

  const rounds = [...new Set(match.clues.map((clue) => clue.round))].sort((a, b) => a - b)

  return (
    <div className={s.table}>
      <section className={s.status} aria-live="polite">
        <div>
          <p className={s.phase}>{over ? PHASE_LABELS.END : PHASE_LABELS[match.phase]}</p>
          <p className={s.meta}>
            Tour {match.currentRound} · votes {match.votesUsed}
            {match.rules.maxVotes === null ? '' : `/${match.rules.maxVotes}`}
          </p>
        </div>
        {!over && remaining !== null && (
          <p className={remaining <= 5 ? `${s.timer} ${s.timerLow}` : s.timer}>{remaining}s</p>
        )}
      </section>

      {!over && (
        <section className={s.secret} aria-label="Votre mot">
          {/* Libellé et aide identiques pour tous les rôles : rien ne doit trahir Mr White à l'écran. */}
          <p className={s.secretLabel}>Votre mot</p>
          <SecretWord word={match.iAmMrWhite ? 'Mr White' : (match.myWord ?? '')} />
          <p className={s.hint}>Écoutez les indices des autres et fondez-vous dans la masse.</p>
          {match.iAmEliminated && <p className={s.eliminatedBanner}>Vous êtes éliminé·e — vous suivez la partie.</p>}
        </section>
      )}

      {over && (
        <section className={s.result} role="status">
          <p className={s.resultTitle}>{RESULT_LABELS[match.status as keyof typeof RESULT_LABELS]}</p>
          {me?.role && (
            <p className={s.resultMine}>
              Vous étiez {ROLE_LABELS[me.role]} —{' '}
              {(me.role === 'AGENT') === (match.status === 'AGENTS_WIN') ? 'victoire !' : 'défaite.'}
            </p>
          )}
          <p className={s.words}>
            Mot des agents : <strong>{match.agentsWord}</strong> · mot de l’undercover :{' '}
            <strong>{match.undercoverWord}</strong>
          </p>
          <Button variant="primary" onClick={onClose}>
            Retour au lobby
          </Button>
        </section>
      )}

      {!over && (
        <section className={s.action}>
          {match.phase === 'DESCRIPTION' &&
            (myTurn ? (
              // Une clé par tour de parole : le champ repart vide à chaque passage.
              <WordForm
                key={`clue-${match.currentRound}-${match.currentSpeakerSeat}`}
                id="uc-word"
                label="À vous : donnez un indice"
                submitLabel="Dire"
                maxLength={50}
                pending={pending}
                onSubmit={onClue}
              />
            ) : (
              <p className={s.prompt}>
                {nameOf(bySeat.get(match.currentSpeakerSeat ?? -1))} cherche son indice…
              </p>
            ))}

          {match.phase === 'VOTE' && (
            <p className={s.prompt}>
              {revote && `Égalité ! Revote entre ${match.tiedSeats.map((seat) => nameOf(bySeat.get(seat))).join(' et ')}. `}
              {match.iAmEliminated
                ? 'La table vote.'
                : iVoted
                  ? 'Vote enregistré, en attente des autres.'
                  : 'Cliquez sur le joueur que vous soupçonnez.'}{' '}
              ({match.votedSeats.length}/{alive.length})
            </p>
          )}

          {match.phase === 'MR_WHITE_GUESS' &&
            (iGuess ? (
              <WordForm
                id="uc-guess"
                label="Démasqué ! Devinez le mot des agents pour gagner"
                submitLabel="Deviner"
                maxLength={100}
                pending={pending}
                onSubmit={onGuess}
              />
            ) : (
              <p className={s.prompt}>
                {nameOf(bySeat.get(match.guesserSeat ?? -1))} était Mr White et tente de deviner le mot des agents…
              </p>
            ))}
        </section>
      )}

      <section aria-label="Joueurs">
        <ul className={s.players}>
          {match.players.map((player) => {
            const votable = isVotable(player)
            const classes = [s.player]
            if (player.seat === match.currentSpeakerSeat && match.phase === 'DESCRIPTION') classes.push(s.speaking)
            if (player.eliminated) classes.push(s.out)
            if (votable) classes.push(s.votable)

            const content = (
              <>
                <span className={s.playerName}>
                  {nameOf(player)}
                  {player.seat === match.mySeat && <span className={s.you}> (vous)</span>}
                </span>
                <span className={s.playerState}>
                  {player.role !== null && <span className={s.role}>{ROLE_LABELS[player.role]}</span>}
                  {player.leftMatch && <span className={s.tagMuted}>Parti</span>}
                  {!player.leftMatch && player.eliminated && <span className={s.tagMuted}>Éliminé</span>}
                  {match.phase === 'VOTE' && match.votedSeats.includes(player.seat) && (
                    <span className={s.voted}>a voté</span>
                  )}
                </span>
              </>
            )

            return (
              <li key={player.seat}>
                {votable ? (
                  <button
                    type="button"
                    className={classes.join(' ')}
                    disabled={pending}
                    onClick={() => onVote(player.seat)}
                    aria-label={`Voter contre ${nameOf(player)}`}
                  >
                    {content}
                  </button>
                ) : (
                  <div className={classes.join(' ')}>{content}</div>
                )}
              </li>
            )
          })}
        </ul>
      </section>

      <section aria-labelledby="uc-clues-title">
        <p className={s.blockTitle} id="uc-clues-title">
          Indices
        </p>
        {rounds.length === 0 ? (
          <p className={s.hint}>Aucun indice pour l’instant.</p>
        ) : (
          <div className={s.clues}>
            {rounds.map((round) => (
              <div key={round} className={s.clueRound}>
                <p className={s.clueRoundTitle}>Tour {round}</p>
                <ul className={s.clueList}>
                  {match.clues
                    .filter((clue) => clue.round === round)
                    .map((clue) => (
                      <li key={`${clue.round}-${clue.seat}`} className={s.clue}>
                        <span className={s.clueAuthor}>{nameOf(bySeat.get(clue.seat))}</span>
                        <span className={s.clueWord}>{clue.word}</span>
                      </li>
                    ))}
                </ul>
              </div>
            ))}
          </div>
        )}
      </section>

      {!over && (
        <Button variant="ghost" onClick={onLeave}>
          Quitter la partie
        </Button>
      )}
    </div>
  )
}

type WordFormProps = {
  id: string
  label: string
  submitLabel: string
  maxLength: number
  pending: boolean
  /** Résout à `true` quand le serveur a accepté : le champ se vide alors. */
  onSubmit: (word: string) => Promise<boolean>
}

/** Un mot à envoyer : l'indice de son tour, ou la devinette de Mr White. */
function WordForm({ id, label, submitLabel, maxLength, pending, onSubmit }: WordFormProps) {
  const [word, setWord] = useState('')

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (await onSubmit(word)) setWord('')
  }

  return (
    <form className={s.wordForm} onSubmit={(event) => void submit(event)}>
      <label className={s.wordLabel} htmlFor={id}>
        {label}
      </label>
      <div className={s.wordRow}>
        <input
          id={id}
          className={s.input}
          value={word}
          maxLength={maxLength}
          autoComplete="off"
          autoFocus
          onChange={(event) => setWord(event.target.value)}
        />
        <Button type="submit" variant="primary" disabled={pending || word.trim() === ''}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
