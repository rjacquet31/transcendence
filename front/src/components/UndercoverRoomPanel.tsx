import { useEffect, useState } from 'react'
import { Check } from 'lucide-react'
import { Button } from './Button.tsx'
import { getUndercoverCategories } from '../lib/actions.ts'
import {
  MAX_CUSTOM_PAIRS,
  MAX_WORD_LENGTH,
  type UndercoverGameMode,
  type UndercoverRoom,
  type UndercoverRules,
  type UndercoverWordPair,
} from '../lib/undercoverClient.ts'
import { categoryLabel, MODE_DESCRIPTIONS, MODE_LABELS } from '../lib/undercoverLabels.ts'
import s from './UndercoverRoomPanel.module.css'

type UndercoverRoomPanelProps = {
  room: UndercoverRoom
  myId: string
  pending: boolean
  onConfigure: (mode: UndercoverGameMode, rules: UndercoverRules | null) => Promise<boolean>
  onChooseWords: (categories: string[], wordPairs: UndercoverWordPair[]) => Promise<boolean>
  onStart: () => void
  onLeave: () => void
}

/** La salle d'attente : le code à partager, les membres, et les réglages —
 *  modifiables par l'hôte seul, lus par les autres. */
export function UndercoverRoomPanel({
  room,
  myId,
  pending,
  onConfigure,
  onChooseWords,
  onStart,
  onLeave,
}: UndercoverRoomPanelProps) {
  const isHost = room.hostId === myId
  const [copied, setCopied] = useState(false)
  const enough = room.members.length >= room.minPlayers

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(room.code)
      setCopied(true)
      setTimeout(() => setCopied(false), 1500)
    } catch {
      /* Presse-papiers refusé (contexte non sécurisé) : le code reste lisible. */
    }
  }

  return (
    <div className={s.layout}>
      <section className={s.card} aria-labelledby="uc-room-code">
        <p className={s.blockTitle} id="uc-room-code">
          Code de la salle
        </p>
        <div className={s.codeRow}>
          <span className={s.code}>{room.code}</span>
          <Button variant="secondary" onClick={() => void copyCode()}>
            {copied ? (
              <>
                Copié <Check size={16} />
              </>
            ) : (
              'Copier'
            )}
          </Button>
        </div>
        <p className={s.hint}>Partagez ce code : vos amis le saisissent pour rejoindre la salle.</p>

        <p className={s.blockTitle}>
          Joueurs · {room.members.length}/{room.maxPlayers}
        </p>
        <ul className={s.members}>
          {room.members.map((member) => (
            <li key={member.userId} className={s.member}>
              <span className={s.memberName}>{member.userName ?? 'Compte supprimé'}</span>
              <span className={s.tags}>
                {member.userId === room.hostId && <span className={s.tag}>Hôte</span>}
                {member.userId === myId && <span className={s.tagMuted}>Vous</span>}
              </span>
            </li>
          ))}
        </ul>

        <div className={s.actions}>
          {isHost ? (
            <Button variant="primary" fullWidth disabled={pending || !enough} onClick={onStart}>
              {enough
                ? 'Lancer la partie'
                : `En attente de joueurs (${room.members.length}/${room.minPlayers})`}
            </Button>
          ) : (
            <p className={s.hint}>L’hôte lancera la partie dès que la table est prête.</p>
          )}
          <Button variant="ghost" fullWidth disabled={pending} onClick={onLeave}>
            Quitter la salle
          </Button>
        </div>
      </section>

      <section className={s.card} aria-labelledby="uc-room-settings">
        <p className={s.blockTitle} id="uc-room-settings">
          Mode de jeu
        </p>
        {isHost ? (
          /* Remonté à chaque vrai changement de règles venu du serveur, ce qui
             repart d'un brouillon neuf. La clé ne dépend que des règles : la salle
             est renvoyée entière à chaque arrivée d'un joueur, et remonter sur
             l'objet effacerait ce que l'hôte est en train de saisir. */
          <HostSettings
            key={JSON.stringify(room.rules)}
            room={room}
            pending={pending}
            onConfigure={onConfigure}
          />
        ) : (
          <>
            <p className={s.modeName}>{MODE_LABELS[room.mode]}</p>
            <p className={s.hint}>{MODE_DESCRIPTIONS[room.mode]}</p>
            <RulesSummary rules={room.rules} />
          </>
        )}

        <p className={s.blockTitle}>Mots</p>
        {isHost ? (
          <HostWords room={room} pending={pending} onChooseWords={onChooseWords} />
        ) : (
          <p className={s.hint}>
            {room.customWordCount > 0
              ? `Liste de l’hôte · ${room.customWordCount} paire${room.customWordCount > 1 ? 's' : ''}`
              : room.categories.length > 0
                ? `Catégories : ${room.categories.map(categoryLabel).join(', ')}`
                : 'Toutes les catégories'}
          </p>
        )}
      </section>
    </div>
  )
}

type HostSettingsProps = {
  room: UndercoverRoom
  pending: boolean
  onConfigure: (mode: UndercoverGameMode, rules: UndercoverRules | null) => Promise<boolean>
}

function HostSettings({ room, pending, onConfigure }: HostSettingsProps) {
  /* Brouillon local des règles personnalisées : rien ne part au serveur avant
     « Enregistrer », pour ne pas pousser un réglage à chaque frappe. */
  const [draft, setDraft] = useState<UndercoverRules>(room.rules)
  const [saved, setSaved] = useState(false)

  const set = <K extends keyof UndercoverRules>(key: K, value: UndercoverRules[K]) => {
    setSaved(false)
    setDraft((previous) => ({ ...previous, [key]: value }))
  }

  const save = async () => {
    if (await onConfigure('CUSTOM', draft)) setSaved(true)
  }

  return (
    <>
      <div className={s.modes} role="group" aria-label="Mode de jeu">
        {(Object.keys(MODE_LABELS) as UndercoverGameMode[]).map((mode) => (
          <Button
            key={mode}
            variant={room.mode === mode ? 'primary' : 'secondary'}
            fullWidth
            disabled={pending}
            onClick={() => void onConfigure(mode, mode === 'CUSTOM' ? draft : null)}
          >
            {MODE_LABELS[mode]}
          </Button>
        ))}
      </div>
      <p className={s.hint}>{MODE_DESCRIPTIONS[room.mode]}</p>

      {room.mode !== 'CUSTOM' ? (
        <RulesSummary rules={room.rules} />
      ) : (
        <form
          className={s.form}
          onSubmit={(event) => {
            event.preventDefault()
            void save()
          }}
        >
          <NumberField
            label="Undercovers"
            value={draft.undercoverCount}
            min={1}
            onChange={(value) => set('undercoverCount', value)}
          />
          <NumberField
            label="Mr White"
            value={draft.mrWhiteCount}
            min={0}
            onChange={(value) => set('mrWhiteCount', value)}
          />
          <NumberField
            label="Tours d’indices avant chaque vote"
            value={draft.descriptionRounds}
            min={1}
            onChange={(value) => set('descriptionRounds', value)}
          />
          <label className={s.field}>
            <span className={s.label}>Nombre de votes</span>
            <span className={s.inline}>
              <input
                type="checkbox"
                checked={draft.maxVotes === null}
                onChange={(event) => set('maxVotes', event.target.checked ? null : 1)}
              />
              <span>Illimité</span>
              {draft.maxVotes !== null && (
                <input
                  className={s.input}
                  type="number"
                  min={1}
                  value={draft.maxVotes}
                  onChange={(event) => set('maxVotes', Number(event.target.value))}
                />
              )}
            </span>
          </label>
          <NumberField
            label="Secondes par indice"
            value={draft.descriptionSeconds}
            min={15}
            onChange={(value) => set('descriptionSeconds', value)}
          />
          <NumberField
            label="Secondes pour voter"
            value={draft.voteSeconds}
            min={15}
            onChange={(value) => set('voteSeconds', value)}
          />
          <NumberField
            label="Secondes pour la devinette de Mr White"
            value={draft.mrWhiteVoteSeconds}
            min={15}
            onChange={(value) => set('mrWhiteVoteSeconds', value)}
          />
          <NumberField
            label="Revotes en cas d’égalité"
            value={draft.tieRevotes}
            min={0}
            onChange={(value) => set('tieRevotes', value)}
          />
          <CheckField
            label="Tirage au sort si l’égalité persiste"
            checked={draft.randomOnUnresolvedTie}
            onChange={(value) => set('randomOnUnresolvedTie', value)}
          />
          <CheckField
            label="Les imposteurs gagnent à égalité avec les agents"
            checked={draft.undercoverWinOnParity}
            onChange={(value) => set('undercoverWinOnParity', value)}
          />
          <Button type="submit" variant="primary" fullWidth disabled={pending}>
            {saved ? (
              <>
                Réglages enregistrés <Check size={16} />
              </>
            ) : (
              'Enregistrer les réglages'
            )}
          </Button>
          <p className={s.hint}>Les règles sont vérifiées au lancement, selon le nombre de joueurs.</p>
        </form>
      )}
    </>
  )
}

/** Une paire par ligne, les deux mots séparés par ce caractère. */
const PAIR_SEPARATOR = '/'

const PAIR_EXAMPLE = ['Naruto/Sasuke', 'Pizza/Burger', 'Messi/Ronaldo'].join('\n')

/** Lit la liste saisie. Les lignes vides sont ignorées ; toute autre ligne doit
 *  donner deux mots, sinon elle est signalée. Le back revérifie tout. */
function parseWordList(text: string) {
  const pairs: UndercoverWordPair[] = []
  const errors: string[] = []

  text.split('\n').forEach((raw, index) => {
    const line = raw.trim()
    if (line === '') return

    const words = line.split(PAIR_SEPARATOR).map((word) => word.trim())
    const where = `Ligne ${index + 1}`
    if (words.length !== 2 || words[0] === '' || words[1] === '') {
      errors.push(`${where} : écrivez deux mots séparés par « ${PAIR_SEPARATOR} ».`)
    } else if (words.some((word) => word.length > MAX_WORD_LENGTH)) {
      errors.push(`${where} : ${MAX_WORD_LENGTH} caractères au plus par mot.`)
    } else if (words[0].toLowerCase() === words[1].toLowerCase()) {
      errors.push(`${where} : les deux mots doivent être différents.`)
    } else {
      pairs.push({ agentsWord: words[0], undercoverWord: words[1] })
    }
  })

  if (pairs.length > MAX_CUSTOM_PAIRS) {
    errors.push(`${MAX_CUSTOM_PAIRS} paires au maximum, ${pairs.length} saisies.`)
  }
  return { pairs, errors }
}

function toWordList(pairs: UndercoverWordPair[]) {
  return pairs.map((pair) => `${pair.agentsWord}${PAIR_SEPARATOR}${pair.undercoverWord}`).join('\n')
}

/** Au-delà, les erreurs suivantes attendent que les premières soient corrigées. */
const SHOWN_ERRORS = 5

type HostWordsProps = {
  room: UndercoverRoom
  pending: boolean
  onChooseWords: (categories: string[], wordPairs: UndercoverWordPair[]) => Promise<boolean>
}

function HostWords({ room, pending, onChooseWords }: HostWordsProps) {
  const savedPairs = room.customWordPairs ?? []
  const [categories, setCategories] = useState<string[]>([])
  const [useCustom, setUseCustom] = useState(room.customWordCount > 0)
  /* Brouillon local : le texte reste là si l'hôte repasse aux mots du jeu puis
     revient à sa liste. */
  const [text, setText] = useState(() => toWordList(savedPairs))

  useEffect(() => {
    /* Sans la liste, on peut toujours jouer : toutes les catégories ou sa liste. */
    getUndercoverCategories()
      .then(setCategories)
      .catch(() => {})
  }, [])

  const toggleCategory = (category: string) => {
    const next = room.categories.includes(category)
      ? room.categories.filter((chosen) => chosen !== category)
      : [...room.categories, category]
    void onChooseWords(next, [])
  }

  const pickGameWords = () => {
    setUseCustom(false)
    if (savedPairs.length > 0) void onChooseWords(room.categories, [])
  }

  const { pairs, errors } = parseWordList(text)
  const dirty = JSON.stringify(pairs) !== JSON.stringify(savedPairs)
  /* Les catégories cochées restent affichées même si le chargement a échoué. */
  const shownCategories = [...new Set([...categories, ...room.categories])]

  return (
    <>
      <div className={s.sources} role="group" aria-label="Origine des mots">
        <Button variant={useCustom ? 'secondary' : 'primary'} fullWidth disabled={pending} onClick={pickGameWords}>
          Mots du jeu
        </Button>
        <Button
          variant={useCustom ? 'primary' : 'secondary'}
          fullWidth
          disabled={pending}
          onClick={() => setUseCustom(true)}
        >
          Ma liste
        </Button>
      </div>

      {!useCustom ? (
        <fieldset className={s.categories}>
          <legend className={s.label}>Catégories</legend>
          <div className={s.chips}>
            {shownCategories.map((category) => (
              <label key={category} className={s.chip}>
                <input
                  type="checkbox"
                  checked={room.categories.includes(category)}
                  disabled={pending}
                  onChange={() => toggleCategory(category)}
                />
                <span>{categoryLabel(category)}</span>
              </label>
            ))}
          </div>
          <p className={s.hint}>
            {room.categories.length === 0
              ? 'Aucune case cochée : les mots viennent de toutes les catégories.'
              : `Les mots sont tirés parmi ${room.categories.length} catégorie${room.categories.length > 1 ? 's' : ''}.`}
          </p>
        </fieldset>
      ) : (
        <form
          className={s.form}
          onSubmit={(event) => {
            event.preventDefault()
            if (pairs.length > 0 && errors.length === 0 && dirty) void onChooseWords(room.categories, pairs)
          }}
        >
          <label className={s.field}>
            <span className={s.label}>Une paire par ligne : mot1{PAIR_SEPARATOR}mot2</span>
            <textarea
              className={s.wordList}
              rows={8}
              value={text}
              placeholder={PAIR_EXAMPLE}
              spellCheck={false}
              onChange={(event) => setText(event.target.value)}
            />
          </label>

          {errors.length > 0 && (
            <ul className={s.errors}>
              {errors.slice(0, SHOWN_ERRORS).map((error) => (
                <li key={error}>{error}</li>
              ))}
            </ul>
          )}

          <p className={s.hint}>
            {pairs.length} paire{pairs.length > 1 ? 's' : ''}. Chaque partie en tire une au hasard, et le
            camp qui reçoit chaque mot est lui aussi tiré au sort.
            {savedPairs.length === 0 && ' Tant que la liste n’est pas enregistrée, les mots du jeu restent utilisés.'}
          </p>

          <Button
            type="submit"
            variant="primary"
            fullWidth
            disabled={pending || pairs.length === 0 || errors.length > 0 || !dirty}
          >
            {!dirty && savedPairs.length > 0 ? (
              <>
                Liste enregistrée <Check size={16} />
              </>
            ) : (
              'Enregistrer la liste'
            )}
          </Button>
        </form>
      )}
    </>
  )
}

function NumberField({
  label,
  value,
  min,
  onChange,
}: {
  label: string
  value: number
  min: number
  onChange: (value: number) => void
}) {
  return (
    <label className={s.field}>
      <span className={s.label}>{label}</span>
      <input
        className={s.input}
        type="number"
        min={min}
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
      />
    </label>
  )
}

function CheckField({
  label,
  checked,
  onChange,
}: {
  label: string
  checked: boolean
  onChange: (value: boolean) => void
}) {
  return (
    <label className={s.inline}>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <span>{label}</span>
    </label>
  )
}

/** Les règles en lecture seule, pour les modes préréglés et les non-hôtes. */
export function RulesSummary({ rules }: { rules: UndercoverRules }) {
  const rows: [string, string][] = [
    ['Undercovers', String(rules.undercoverCount)],
    ['Mr White', rules.mrWhiteCount === 0 ? 'Aucun' : String(rules.mrWhiteCount)],
    ['Tours d’indices avant un vote', String(rules.descriptionRounds)],
    ['Votes', rules.maxVotes === null ? 'Illimités' : String(rules.maxVotes)],
    ['Chrono indice / vote', `${rules.descriptionSeconds} s / ${rules.voteSeconds} s`],
    [
      'Égalité',
      `${rules.tieRevotes} revote${rules.tieRevotes > 1 ? 's' : ''}, puis ${
        rules.randomOnUnresolvedTie ? 'tirage au sort' : 'personne n’est éliminé'
      }`,
    ],
    ['Victoire des imposteurs à égalité', rules.undercoverWinOnParity ? 'Oui' : 'Non'],
  ]

  return (
    <dl className={s.rules}>
      {rows.map(([term, value]) => (
        <div key={term} className={s.ruleRow}>
          <dt>{term}</dt>
          <dd>{value}</dd>
        </div>
      ))}
    </dl>
  )
}
