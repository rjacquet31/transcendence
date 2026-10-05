/** Files STOMP des notifications, rien n'est stocké : un compte hors ligne retrouve l'état sur la page Amis. */
export const FRIEND_NOTIFICATIONS = '/user/queue/notifications.friends'
export const CHAT_NOTIFICATIONS = '/user/queue/notifications.chat'

/** `fromUserId` est toujours l'autre bout de la relation, l'id des routes `/friend-list`. */
export type FriendNotificationDto = {
  type: 'REQUEST_RECEIVED' | 'REQUEST_ACCEPTED'
  fromUserId: string
  fromUserName: string
}

/** Envoyé au seul destinataire, avec un aperçu déjà tronqué par le back. */
export type ChatNotificationDto = {
  messageId: number
  senderId: string
  senderUserName: string
  preview: string
  sentAt: string
}
