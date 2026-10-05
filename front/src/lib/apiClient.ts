const BASE_URL = '/api'

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/** Message affichable d'une erreur d'appel, ou `fallback` si elle vient d'ailleurs. */
export function errorMessage(err: unknown, fallback: string) {
  return err instanceof ApiError ? err.message : fallback
}

/** Un ObjectError Spring sérialisé (le body de `bindingResult.getAllErrors()`). */
type SpringError = { field?: string; defaultMessage?: string }

/** Cookie `logged_in=1` (sans secret) posé par le back : absent, pas de session à restaurer, donc pas de route protégée (évite un 401 en console). */
export function hasSessionHint() {
  return document.cookie.split('; ').includes('logged_in=1')
}

/** Un seul refresh à la fois : les requêtes qui se croisent attendent la même promesse. */
let refreshing: Promise<unknown> | null = null

/** Le back refuse de trois façons : tableau d'ObjectError (`@Valid`), `{ error }`, ou map champ → message. */
function parseErrorBody(status: number, body: unknown): ApiError | null {
  const fieldErrors: Record<string, string> = {}

  if (Array.isArray(body)) {
    for (const item of body as SpringError[]) {
      if (item?.field && typeof item.defaultMessage === 'string') {
        fieldErrors[item.field] = item.defaultMessage
      }
    }
    if (Object.keys(fieldErrors).length === 0) {
      const first = (body as SpringError[])[0]?.defaultMessage
      return first ? new ApiError(status, first) : null
    }
  } else if (body && typeof body === 'object') {
    const record = body as Record<string, unknown>
    if (typeof record.error === 'string') return new ApiError(status, record.error)

    for (const [key, value] of Object.entries(record)) {
      if (typeof value === 'string') fieldErrors[key] = value
    }
  }

  return Object.keys(fieldErrors).length > 0
    ? new ApiError(status, 'Données invalides.', fieldErrors)
    : null
}

async function parseError(res: Response): Promise<ApiError> {
  // On lit en texte : le back répond parfois en text/plain ("Invalid credentials").
  const raw = await res.text().catch(() => '')

  let body: unknown = null
  try {
    body = JSON.parse(raw)
  } catch {
    body = null
  }

  return (
    parseErrorBody(res.status, body) ??
    new ApiError(res.status, raw.trim() || `Erreur ${res.status} (${res.statusText}).`)
  )
}

/** Certaines routes répondent 204, vide ou text/plain : parser en JSON d'office ferait planter une requête réussie. */
async function readBody(res: Response): Promise<unknown> {
  const text = await res.text()
  if (!text) return undefined
  if (!(res.headers.get('Content-Type') ?? '').includes('application/json')) return text
  return JSON.parse(text)
}

async function request<T>(path: string, method: string, body?: unknown, retried = false): Promise<T> {
  // Un FormData : le navigateur pose lui-même le Content-Type (boundary), on ne force rien.
  const isForm = body instanceof FormData

  // /auth/* est public sauf /auth/2fa/* (session requise, hors /auth/2fa/login) ; tout le reste est protégé.
  const needsSession = path.startsWith('/auth/2fa/') && path !== '/auth/2fa/login'
  const isAuthRoute = path.startsWith('/auth/') && !needsSession
  if (!isAuthRoute && !hasSessionHint()) throw new ApiError(401, 'Non connecté.')

  const headers = new Headers()
  if (body !== undefined && !isForm) headers.set('Content-Type', 'application/json')

  let res: Response
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      credentials: 'include', // indispensable pour le cookie de session / JWT
      body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Impossible de joindre le serveur.')
  }

  // Access_token expiré : refresh avec le refresh_token (7 jours) puis rejeu unique (hors routes publiques /auth/*).
  if ((res.status === 401 || res.status === 403) && !retried && !isAuthRoute) {
    refreshing ??= request('/auth/refresh-token', 'POST').finally(() => {
      refreshing = null
    })
    try {
      await refreshing
    } catch {
      // Refresh token expiré ou révoqué : c'est une vraie déconnexion.
      throw await parseError(res)
    }
    return request<T>(path, method, body, true)
  }

  if (!res.ok) throw await parseError(res)

  return (await readBody(res)) as T
}

export const api = {
  get: <T>(path: string) => request<T>(path, 'GET'),
  post: <T>(path: string, body?: unknown) => request<T>(path, 'POST', body),
  postForm: <T>(path: string, form: FormData) => request<T>(path, 'POST', form),
  put: <T>(path: string, body?: unknown) => request<T>(path, 'PUT', body),
}
