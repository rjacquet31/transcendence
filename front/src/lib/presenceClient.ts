/** File STOMP de présence : seuls les amis acceptés déjà connectés sont prévenus, l'état initial vient de `getFriendList()`. */
export const PRESENCE = '/user/queue/presence'

/** Miroir du `PresenceDto` du back. `lastSeen` n'est rempli qu'au départ. */
export type PresenceDto = {
  idUser: string
  online: boolean
  lastSeen: string | null
}
