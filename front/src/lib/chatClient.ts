import { api } from './apiClient.ts'

export type ChatMessageDto = {
  id: number
  senderId: string
  receiverId: string
  content: string
  sentAt: string
  isRead: boolean
}

export type ChatHistoryDto = {
  messages: ChatMessageDto[]
  nextCursor: number | null
  hasMore: boolean
}

export const CHAT_MESSAGES = '/user/queue/messages'
export const CHAT_ERRORS = '/user/queue/errors'
export const CHAT_SEND = '/app/chat.send'

const HISTORY_PAGE_SIZE = 50

export function sendChatMessage(
  publish: (destination: string, body: unknown) => boolean,
  receiverId: string,
  content: string,
) {
  return publish(CHAT_SEND, { receiverId, content })
}

export function getChatHistory(
  userId: string,
  before: number | null = null,
  limit: number = HISTORY_PAGE_SIZE,
) {
  const params = new URLSearchParams({ limit: String(limit) })
  if (before !== null) params.set('before', String(before))
  return api.get<ChatHistoryDto>(`/chat/${userId}?${params.toString()}`)
}

export function markConversationRead(userId: string) {
  return api.put<{ updated: number }>(`/chat/${userId}/read`)
}

export function mergeMessages(current: ChatMessageDto[], incoming: ChatMessageDto[]) {
  const byId = new Map<number, ChatMessageDto>()
  for (const message of current) byId.set(message.id, message)
  for (const message of incoming) byId.set(message.id, message)

  return [...byId.values()].sort((a, b) => {
    const delta = new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime()
    return delta !== 0 ? delta : a.id - b.id
  })
}