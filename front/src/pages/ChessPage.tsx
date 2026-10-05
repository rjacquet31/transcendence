import { Chess, type Square } from 'chess.js'
import { useEffect, useState } from 'react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { Button } from '../components/Button.tsx'
import { CapturedPieces } from '../components/CapturedPieces.tsx'
import { ChessBoard, type BoardOrientation } from '../components/ChessBoard.tsx'
import { ChessBotModal } from '../components/ChessBotModal.tsx'
import { ChessInviteModal } from '../components/ChessInviteModal.tsx'
import { ChessPromotionModal, type PromotionPiece } from '../components/ChessPromotionModal.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { getUserProfile, sendChessInvitation } from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import { START_FEN, capturesOf, squaresOf } from '../lib/chessBoard.ts'
import type { ChessMatchDto, ChessSide } from '../lib/chessClient.ts'
import { useChessMatch } from '../ws/useChessMatch.ts'
import page from '../style/page.module.css'
import s from './ChessPage.module.css'

type ChessPageProps = {
  onNavigate: (action: string) => void
}

type Snapshot = {
  squares: (string | null)[]
  turn: 'w' | 'b'
  check: boolean
  fen: string
  history: { san: string; lan: string }[]
}

type Round = { number: number; white: string; black: string | null }

/** Fige l'état de la partie (cases, trait, échec, FEN, historique) en un objet neuf pour React. */
function snapshot(chess: Chess): Snapshot {
  return {
    squares: squaresOf(chess),
    turn: chess.turn(),
    check: chess.isCheck(),
    fen: chess.fen(),
    history: chess.history({ verbose: true }).map(({ san, lan }) => ({ san, lan })),
  }
}

/** Crée une partie vierge à la position de départ, avec son instantané d'affichage. */
function newGame() {
  const chess = new Chess()
  return { chess, view: snapshot(chess) }
}

/** Compare deux FEN sur les seuls champs de position (pièces, trait, roques). */
function samePosition(left: string, right: string) {
  return left.split(' ').slice(0, 3).join(' ') === right.split(' ').slice(0, 3).join(' ')
}

/** Copie une partie avec son historique, ou depuis son seul FEN si le PGN manque (partie reprise après rechargement). */
function cloneGame(chess: Chess) {
  const copy = new Chess()

  try {
    copy.loadPgn(chess.pgn())
    return copy
  } catch {
    return new Chess(chess.fen())
  }
}

/** Rejoue le coup nommé par le serveur, s'il mène bien à la position attendue. */
function replay(chess: Chess, move: string, expectedFen: string) {
  const copy = cloneGame(chess)

  try {
    copy.move({
      from: move.slice(0, 2) as Square,
      to: move.slice(2, 4) as Square,
      promotion: move.length > 4 ? move[4] : undefined,
    })
  } catch {
    return null
  }

  return samePosition(copy.fen(), expectedFen) ? copy : null
}

/** Personne n'a nommé le coup, mais un seul coup légal mène à cette position : celui du bot. */
function catchUp(chess: Chess, expectedFen: string) {
  const copy = cloneGame(chess)

  for (const move of copy.moves()) {
    copy.move(move)
    if (samePosition(copy.fen(), expectedFen)) return copy
    copy.undo()
  }

  return null
}

/** Regroupe l'historique en tours numérotés (un coup blanc, un coup noir) pour la liste des coups. */
function toRounds(history: { san: string }[]): Round[] {
  const rounds: Round[] = []

  for (let i = 0; i < history.length; i += 2) {
    rounds.push({
      number: i / 2 + 1,
      white: history[i].san,
      black: history[i + 1]?.san ?? null,
    })
  }

  return rounds
}

/** Traduit le statut de la partie en message de fin (mat, abandon, pat, nulle), ou null si elle continue. */
function outcomeOfMatch(match: ChessMatchDto) {
  if (match.status === 'IN_PROGRESS') return null

  const winner = match.winnerSide === 'WHITE' ? 'les blancs' : 'les noirs'

  if (match.status === 'CHECKMATE') return `Échec et mat — ${winner} gagnent`
  if (match.status === 'RESIGNED') return `Abandon — ${winner} gagnent`
  if (match.status === 'STALEMATE') return 'Pat — partie nulle'

  return 'Partie nulle'
}

/** Affiche la liste numérotée des coups joués, ou un message si aucun coup n'a été joué. */
function MoveList({ rounds }: { rounds: Round[] }) {
  if (rounds.length === 0) return <p className={s.empty}>Aucun coup pour l'instant.</p>

  return (
    <ol className={s.moveList}>
      {rounds.map((round) => (
        <li key={round.number} className={s.round}>
          <span className={s.roundNumber}>{round.number}.</span>
          <span>{round.white}</span>
          <span>{round.black ?? ''}</span>
        </li>
      ))}
    </ol>
  )
}

/** Page Échecs : sans partie serveur le plateau reste au départ et inerte ; avec partie, chess.js applique les règles pour l'affichage, le serveur reste juge. */
export function ChessPage({ onNavigate }: ChessPageProps) {
  const { user, isLoggedIn, authChecked } = useAuthModal()
  const online = useChessMatch()
  const { match, mySide, isMyTurn, serverMove } = online

  const [game, setGame] = useState(newGame)
  const [selected, setSelected] = useState<string | null>(null)
  const [targets, setTargets] = useState<string[]>([])
  const [lastMove, setLastMove] = useState<{ from: string; to: string } | null>(null)
  /** Coup de promotion en attente de sa pièce : la modale reste ouverte tant qu'il est posé. */
  const [promotion, setPromotion] = useState<{ from: string; to: string } | null>(null)
  const [orientation, setOrientation] = useState<BoardOrientation>('white')
  const [opponentName, setOpponentName] = useState<string | null>(null)

  const [inviteOpen, setInviteOpen] = useState(false)
  /** Pseudo dont l'invitation part en ce moment : verrouille toute la liste. */
  const [invitePending, setInvitePending] = useState<string | null>(null)
  const [inviteError, setInviteError] = useState<string | null>(null)
  const [inviteSent, setInviteSent] = useState<{ userName: string; side: ChessSide } | null>(null)

  const [botOpen, setBotOpen] = useState(false)
  const [botPending, setBotPending] = useState(false)
  const [botError, setBotError] = useState<string | null>(null)

  const { chess, view } = game

  /* On joue de son côté du plateau, sans avoir à le retourner. */
  useEffect(() => {
    if (mySide !== null) setOrientation(mySide === 'BLACK' ? 'black' : 'white')
  }, [mySide])

  /* La position du serveur fait foi : on rejoue le coup reçu pour garder l'historique, sinon on repart du FEN ; sans partie, retour au départ. */
  useEffect(() => {
    if (match === null) {
      setGame((current) => (samePosition(current.view.fen, START_FEN) ? current : newGame()))
      setLastMove(null)
      return
    }

    /* Updater volontairement pur (ne mute rien, ne pose aucun état) : au pire une reconstruction par message, pas de boucle. */
    setGame((current) => {
      if (samePosition(current.view.fen, match.fen)) return current

      const played = serverMove === null ? null : replay(current.chess, serverMove.move, match.fen)
      if (played !== null) return { chess: played, view: snapshot(played) }

      const found = catchUp(current.chess, match.fen)
      if (found !== null) return { chess: found, view: snapshot(found) }

      const fresh = new Chess(match.fen)
      return { chess: fresh, view: snapshot(fresh) }
    })

    if (serverMove !== null) {
      setLastMove({ from: serverMove.move.slice(0, 2), to: serverMove.move.slice(2, 4) })
    }
  }, [match, serverMove])

  /* Le back ne donne que l'UUID des joueurs ; un camp tenu par un bot s'annonce par son niveau. */
  useEffect(() => {
    if (match === null || mySide === null) {
      setOpponentName(null)
      return
    }

    const opponentId = mySide === 'WHITE' ? match.blackPlayerId : match.whitePlayerId
    if (opponentId === null) {
      const level = mySide === 'WHITE' ? match.blackBotLevel : match.whiteBotLevel
      setOpponentName(level === null ? 'Bot' : `Bot · niveau ${level}`)
      return
    }

    let alive = true
    getUserProfile(opponentId)
      .then((profile) => {
        if (alive) setOpponentName(profile.userName)
      })
      .catch(() => {
        if (alive) setOpponentName(null)
      })

    return () => {
      alive = false
    }
  }, [match, mySide])

  const running = match !== null && match.status === 'IN_PROGRESS'
  const outcome = match === null ? null : outcomeOfMatch(match)
  /* Trait du plateau affiché, déjà avancé si on vient de jouer sans réponse du serveur. */
  const localSide: ChessSide = view.turn === 'w' ? 'WHITE' : 'BLACK'
  /* Le plateau ne répond pas sans adversaire, hors de son tour, ou en attendant la réponse à son coup (surtout contre le bot). */
  const playable = running && isMyTurn && localSide === mySide

  /* Filet : si la partie s'arrête modale ouverte (abandon adverse, déconnexion), on la ferme. */
  useEffect(() => {
    if (!playable) setPromotion(null)
  }, [playable])

  /* Dernier coup de la position affichée, lu dans l'historique (couvre le bot) ; `lastMove` ne sert qu'aux parties reprises depuis un FEN. */
  const played = view.history.at(-1)
  const highlighted =
    played === undefined ? lastMove : { from: played.lan.slice(0, 2), to: played.lan.slice(2, 4) }

  /** Sélectionne la case si elle porte une pièce jouable et calcule ses arrivées légales. */
  const select = (square: string) => {
    const moves = chess.moves({ square: square as Square, verbose: true })
    if (moves.length === 0) return

    setSelected(square)
    setTargets(moves.map((move) => move.to))
  }

  /** Désélectionne la case courante et efface ses cases d'arrivée. */
  const clearSelection = () => {
    setSelected(null)
    setTargets([])
  }

  /** Joue le coup localement puis l'envoie au serveur ; `piece` (promotion seulement) devient la dernière lettre de l'UCI (`e7e8n`). */
  const playMove = (from: string, to: string, piece?: PromotionPiece) => {
    if (match === null) return

    const move = chess.move({ from: from as Square, to: to as Square, promotion: piece })

    setGame({ chess, view: snapshot(chess) })
    setLastMove({ from, to })
    clearSelection()

    /* Affichage immédiat : chess.js a validé le coup, et un refus du back nous ramènerait sur sa position. */
    void online.play(match.matchId, move.lan)
  }

  /** Gère un clic sur une case : sélectionne une pièce, change de sélection, ou joue le coup. */
  const handleSquareClick = (square: string) => {
    if (!playable || match === null) return

    if (selected === null) {
      select(square)
      return
    }

    if (square === selected) {
      clearSelection()
      return
    }

    /* Pas une arrivée légale : autre pièce à nous (on change de sélection) ou clic sans sens (ignoré). */
    if (!targets.includes(square)) {
      select(square)
      return
    }

    /* Un pion arrivé au bout doit choisir sa pièce : le coup reste en suspens, la modale le reprend. */
    const promoting = chess
      .moves({ square: selected as Square, verbose: true })
      .some((move) => move.to === square && move.promotion !== undefined)

    if (promoting) {
      setPromotion({ from: selected, to: square })
      return
    }

    playMove(selected, square)
  }

  /** Joue le coup de promotion en attente avec la pièce choisie, puis ferme la modale. */
  const promoteTo = (piece: PromotionPiece) => {
    if (promotion === null) return

    playMove(promotion.from, promotion.to, piece)
    setPromotion(null)
  }

  /** Refermer sans choisir annule le coup (rien n'est parti) et rend la case à la sélection. */
  const cancelPromotion = () => {
    setPromotion(null)
    if (promotion !== null) select(promotion.from)
  }

  /** Sans ce `leave`, la resynchronisation ramènerait la position finale et le back interdirait toute invitation. */
  const leaveMatch = () => {
    online.leave()
    clearSelection()
  }

  /** Ouvre la modale de partie contre le bot, sans erreur résiduelle. */
  const openBot = () => {
    setBotError(null)
    setBotOpen(true)
  }

  /** Un refus du back laisse la modale ouverte, le message se lisant à côté du bouton actionné. */
  const startBot = async (side: ChessSide, botLvl: number) => {
    setBotPending(true)
    setBotError(null)

    try {
      await online.startBot(side, botLvl)
      clearSelection()
      setBotOpen(false)
    } catch (err: unknown) {
      setBotError(errorMessage(err, 'Partie contre le bot impossible.'))
    } finally {
      setBotPending(false)
    }
  }

  /** Ouvre la modale d'invitation, sans erreur ni confirmation résiduelles. */
  const openInvite = () => {
    setInviteError(null)
    setInviteSent(null)
    setInviteOpen(true)
  }

  /** Envoie une invitation de partie à un joueur et mémorise le camp que le back lui a attribué. */
  const invite = async (userName: string, side: ChessSide | null) => {
    setInvitePending(userName)
    setInviteError(null)

    try {
      // Le back renvoie le camp retenu : c'est lui qui tranche en cas d'aléatoire.
      const invitation = await sendChessInvitation(userName, side)
      setInviteSent({ userName, side: invitation.fromSide })
      setInviteOpen(false)
    } catch (err: unknown) {
      setInviteError(errorMessage(err, 'Invitation refusée.'))
    } finally {
      setInvitePending(null)
    }
  }

  const rounds = toRounds(view.history)
  const captures = capturesOf(view.squares)
  const myName = user?.userName ?? 'Vous'
  const theirName = opponentName ?? '…'
  const whiteName = match === null ? '—' : mySide === 'WHITE' ? myName : theirName
  const blackName = match === null ? '—' : mySide === 'BLACK' ? myName : theirName

  /* Après tous les hooks : un visiteur non connecté ne voit pas le plateau. */
  if (!authChecked || !isLoggedIn || !user) {
    return (
      <AppLayout current="nav:chess" onNavigate={onNavigate}>
        <PageHeader title="Échecs" subtitle="Parties en ligne entre amis ou contre le bot." />
        {authChecked && <p className={page.denied}>Connecte-toi pour jouer aux échecs.</p>}
      </AppLayout>
    )
  }

  return (
    <AppLayout current="nav:chess" onNavigate={onNavigate}>
      <PageHeader
        title="Échecs"
        subtitle={
          match === null
            ? 'Invitez un(e) ami(e), ou lancez une partie contre le bot.'
            : 'Partie en ligne : les coups de l’adversaire arrivent en temps réel.'
        }
      />
      <section className={s.layout} aria-labelledby="chess-board-title">
        <div>
          <SectionTitle id="chess-board-title">Plateau</SectionTitle>
          <ChessBoard
            squares={view.squares}
            orientation={orientation}
            selected={selected}
            targets={targets}
            lastMove={highlighted}
            onSquareClick={handleSquareClick}
          />

          {/* Près du plateau : la modale prolonge le coup joué, elle ne fait pas partie des commandes. */}
          <ChessPromotionModal
            open={promotion !== null}
            white={mySide !== 'BLACK'}
            onClose={cancelPromotion}
            onSelect={promoteTo}
          />
        </div>

        <aside className={s.panel} aria-label="Informations de la partie">
          <div className={s.players}>
            <div className={s.player}>
              <p className={s.playerLine}>
                <span className={s.side}>Noirs</span>
                <span className={s.name}>{blackName}</span>
              </p>
              <CapturedPieces pieces={captures.byBlack} advantage={-captures.advantage} />
            </div>
            <div className={s.player}>
              <p className={s.playerLine}>
                <span className={s.side}>Blancs</span>
                <span className={s.name}>{whiteName}</span>
              </p>
              <CapturedPieces pieces={captures.byWhite} advantage={captures.advantage} />
            </div>
          </div>

          {running && (
            <p className={s.turn} aria-live="polite">
              {isMyTurn ? 'À vous de jouer' : 'Trait à l’adversaire'} · coup{' '}
              {Math.floor(view.history.length / 2) + 1}
              {view.check ? ' · échec !' : ''}
            </p>
          )}

          {outcome !== null && (
            <p className={s.over} role="status">
              {outcome}
            </p>
          )}

          {online.error !== null && (
            <p className={s.over} role="status">
              {online.error}
            </p>
          )}

          <div>
            <p className={s.blockTitle}>Coups joués</p>
            <MoveList rounds={rounds} />
          </div>

          <div className={s.actions}>
            {running && (
              /* Seule sortie d'une partie en cours. */
              <Button variant="primary" fullWidth onClick={() => void online.resign(match.matchId)}>
                Abandonner
              </Button>
            )}

            {match !== null && !running && (
              <Button variant="primary" fullWidth onClick={leaveMatch}>
                Quitter la partie
              </Button>
            )}

            <Button variant="primary" fullWidth disabled={running} onClick={openInvite}>
              Inviter un(e) ami(e)
            </Button>

            <Button variant="primary" fullWidth disabled={running} onClick={openBot}>
              Jouer contre le bot
            </Button>

            <ChessBotModal
              open={botOpen}
              pending={botPending}
              error={botError}
              onClose={() => setBotOpen(false)}
              onStart={(side, botLvl) => void startBot(side, botLvl)}
            />

            <ChessInviteModal
              open={inviteOpen}
              pending={invitePending}
              error={inviteError}
              onClose={() => setInviteOpen(false)}
              onInvite={invite}
            />

            <Button
              variant="secondary"
              fullWidth
              onClick={() => setOrientation((side) => (side === 'white' ? 'black' : 'white'))}
            >
              Retourner l'échiquier
            </Button>

            {inviteSent !== null && (
              <p className={s.hint} aria-live="polite">
                Invitation envoyée à {inviteSent.userName} — vous jouerez les{' '}
                {inviteSent.side === 'WHITE' ? 'blancs' : 'noirs'}.
              </p>
            )}
          </div>
        </aside>
      </section>
    </AppLayout>
  )
}
