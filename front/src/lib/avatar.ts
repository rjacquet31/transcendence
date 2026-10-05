/** Le back renvoie un chemin (`/avatars/…`) ou `null`. */
export function avatarSrc(avatarUrl: string | null | undefined) {
  if (!avatarUrl) return null
  return avatarUrl.startsWith('/') ? avatarUrl : `/${avatarUrl}`
}
