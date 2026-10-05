import type {
  UndercoverGameMode,
  UndercoverMatchStatus,
  UndercoverPhase,
  UndercoverRole,
} from './undercoverClient.ts'

export const MODE_LABELS: Record<UndercoverGameMode, string> = {
  CLASSIC: 'Classique',
  BATTLEROYAL: 'Battle royale',
  CUSTOM: 'Personnalisé',
}

export const MODE_DESCRIPTIONS: Record<UndercoverGameMode, string> = {
  CLASSIC:
    '3 tours d’indices, puis un seul vote : si les agents ne trouvent pas l’undercover, ils perdent. Pour les petites tables.',
  BATTLEROYAL:
    'Un tour d’indices, un vote, et on recommence jusqu’à trouver l’undercover — ou jusqu’à ce qu’il égale les agents.',
  CUSTOM: 'Nombre d’imposteurs, Mr White, tours, votes et chronos : vous réglez tout.',
}

/** Les catégories telles qu'elles sont enregistrées en base. Une catégorie
 *  ajoutée plus tard sans libellé s'affiche telle quelle, avec une majuscule. */
const CATEGORY_LABELS: Record<string, string> = {
  general: 'Général',
  anime: 'Anime',
  foot: 'Foot',
  'jeux video': 'Jeux vidéo',
  tech: 'Tech',
  sf: 'Science-fiction',
}

export function categoryLabel(category: string) {
  return CATEGORY_LABELS[category] ?? category.charAt(0).toUpperCase() + category.slice(1)
}

export const ROLE_LABELS: Record<UndercoverRole, string> = {
  AGENT: 'Agent',
  UNDERCOVER: 'Undercover',
  MR_WHITE: 'Mr White',
}

export const PHASE_LABELS: Record<UndercoverPhase, string> = {
  DESCRIPTION: 'Indices',
  VOTE: 'Vote',
  MR_WHITE_GUESS: 'Devinette de Mr White',
  END: 'Partie terminée',
}

export const RESULT_LABELS: Record<Exclude<UndercoverMatchStatus, 'IN_PROGRESS'>, string> = {
  AGENTS_WIN: 'Les agents ont démasqué les imposteurs',
  UNDERCOVER_WIN: 'Les imposteurs l’emportent',
  MR_WHITE_WIN: 'Mr White a deviné le mot des agents',
}
