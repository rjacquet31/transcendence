import { useEffect, useState } from 'react'
import { useAuthModal } from '../auth/authModalContext.ts'
import { AppLayout } from '../components/AppLayout.tsx'
import { PageHeader } from '../components/PageHeader.tsx'
import { SectionTitle } from '../components/SectionTitle.tsx'
import { Tableau, type Colonne } from '../components/Tableau.tsx'
import {
  getMyChessHistory,
  getMyChessStats,
  type ChessHistoryDto,
  type ChessPlayerStatDto,
  type ChessResult,
} from '../lib/actions.ts'
import { errorMessage } from '../lib/apiClient.ts'
import { formatDateTime } from '../lib/format.ts'
import page from '../style/page.module.css'
import s from './StatsPage.module.css'

const RESULT_LABELS: Record<ChessResult, string> = {
  WIN: 'Victoire',
  LOSS: 'Défaite',
  DRAW: 'Nul',
}

const RESULT_CLASSES: Record<ChessResult, string> = {
  WIN: s.win,
  LOSS: s.loss,
  DRAW: s.draw,
}

/** Durée d'une partie en « 12 min 05 s » ; « — » si une borne manque. */
function formatDuration(start: string, end: string | null) {
  if (!end) return '—'
  const ms = new Date(end).getTime() - new Date(start).getTime()
  if (Number.isNaN(ms) || ms < 0) return '—'
  const total = Math.round(ms / 1000)
  const minutes = Math.floor(total / 60)
  const seconds = String(total % 60).padStart(2, '0')
  return minutes > 0 ? `${minutes} min ${seconds} s` : `${total} s`
}

/** Plus récente d'abord ; une partie sans `endedAt` retombe sur `createdAt`. */
function byMostRecent(a: ChessHistoryDto, b: ChessHistoryDto) {
  const time = (row: ChessHistoryDto) => new Date(row.endedAt ?? row.createdAt).getTime()
  return time(b) - time(a)
}

type StatsPageProps = {
  onNavigate: (action: string) => void
}

export function StatsPage({ onNavigate }: StatsPageProps) {
  const { isLoggedIn, authChecked } = useAuthModal()

  const [stats, setStats] = useState<ChessPlayerStatDto | null>(null)
  const [history, setHistory] = useState<ChessHistoryDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!isLoggedIn) return

    let alive = true

    Promise.all([getMyChessStats(), getMyChessHistory()])
      .then(([statList, historyList]) => {
        if (!alive) return
        setStats(statList)
        setHistory([...historyList].sort(byMostRecent))
        setError(null)
      })
      .catch((err: unknown) => {
        if (alive) setError(errorMessage(err, 'Chargement des statistiques impossible.'))
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [isLoggedIn])

  const played = stats ? stats.winCount + stats.lossCount + stats.drawCount : 0
  const winRate = played > 0 && stats ? Math.round((stats.winCount / played) * 100) : 0

  const tiles = stats
    ? [
        { key: 'played', label: 'Parties', value: String(played) },
        { key: 'wins', label: 'Victoires', value: String(stats.winCount) },
        { key: 'losses', label: 'Défaites', value: String(stats.lossCount) },
        { key: 'draws', label: 'Nuls', value: String(stats.drawCount) },
        { key: 'rate', label: 'Taux de victoire', value: `${winRate} %` },
      ]
    : []

  const historyColumns: Colonne<ChessHistoryDto>[] = [
    { key: 'endedAt', header: 'Date', render: (row) => formatDateTime(row.endedAt) },
    { key: 'white', header: 'Blancs', render: (row) => row.player1 },
    { key: 'black', header: 'Noirs', render: (row) => row.player2 },
    {
      key: 'result',
      header: 'Résultat',
      render: (row) => (
        <span className={`${s.badge} ${RESULT_CLASSES[row.result]}`}>
          {RESULT_LABELS[row.result]}
        </span>
      ),
    },
    {
      key: 'duration',
      header: 'Durée',
      render: (row) => formatDuration(row.createdAt, row.endedAt),
    },
  ]

  return (
    <AppLayout current="nav:stats" onNavigate={onNavigate}>
      <PageHeader
        title="Stats & historique"
        subtitle="Ton bilan aux échecs et le détail de tes parties terminées."
      />

      {!authChecked ? null : !isLoggedIn ? (
        <p className={page.denied}>Connecte-toi pour voir tes statistiques.</p>
      ) : loading ? (
        <p className={page.state}>Chargement des statistiques…</p>
      ) : error ? (
        <p className={s.error}>{error}</p>
      ) : (
        <>
          <section aria-labelledby="stats-chess-title">
            <SectionTitle id="stats-chess-title">Échecs — bilan</SectionTitle>
            <ul className={s.tiles}>
              {tiles.map((tile) => (
                <li key={tile.key} className={s.tile}>
                  <span className={s.tileValue}>{tile.value}</span>
                  <span className={page.label}>{tile.label}</span>
                </li>
              ))}
            </ul>
          </section>

          <section className={s.section} aria-labelledby="stats-history-title">
            <SectionTitle id="stats-history-title">
              Historique des parties ({history.length})
            </SectionTitle>
            <Tableau
              caption="Historique de tes parties d’échecs terminées"
              columns={historyColumns}
              rows={history}
              rowKey={(row) => String(row.chessMatchId)}
              emptyText="Aucune partie terminée."
            />
          </section>
        </>
      )}
    </AppLayout>
  )
}
