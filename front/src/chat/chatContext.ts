import { createContext, useContext } from 'react'
import type { FriendListDto } from '../lib/actions.ts'
import type { ChatMessageDto } from '../lib/chatClient.ts'

export type ThreadHistory = {
  loading: boolean
  loaded: boolean
  hasMore: boolean
  cursor: number | null
  error: string | null
}

export const EMPTY_HISTORY: ThreadHistory = {
  loading: false,
  loaded: false,
  hasMore: false,
  cursor: null,
  error: null,
}

export type ChatValue = {
  myId: string | null
  rows: FriendListDto[]
  friends: FriendListDto[]
  loading: boolean
  error: string | null
  refresh: () => Promise<void>
  nameOf: (idUser: string) => string
  avatarOf: (idUser: string) => string | null
  threads: Record<string, ChatMessageDto[]>
  history: Record<string, ThreadHistory>
  historyOf: (idUser: string) => ThreadHistory
  loadHistory: (idUser: string) => void
  loadOlder: (idUser: string) => void
  unread: Record<string, number>
  recentIds: string[]
  chatError: string | null
  clearChatError: () => void
  markRead: (idUser: string) => void
  send: (receiverId: string, content: string) => boolean
  quickChatIds: string[]
  openQuickChat: (idUser: string) => void
  closeQuickChat: (idUser: string) => void
}

export const ChatContext = createContext<ChatValue | null>(null)

export function useChat() {
  const ctx = useContext(ChatContext)
  if (!ctx) throw new Error('useChat doit être utilisé dans ChatProvider')
  return ctx
}