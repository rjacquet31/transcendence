import { api } from './apiClient.ts'
import type { ChessInvitationDto, ChessMatchDto, ChessSide } from './chessClient.ts'
import type {
  UndercoverGameMode,
  UndercoverMatchView,
  UndercoverRoom,
  UndercoverRules,
  UndercoverStats,
  UndercoverWordPair,
} from './undercoverClient.ts'

export type UserPublicProfileDto = {
  id: string
  userName: string
  email: string | null
  online: string | null
  avatarUrl: string | null
  role: string | null
  twoFactorEnabled: boolean | null
  oauth42Linked: boolean | null
}

export type TwoFactorSetupDto = {
  qrCodeDataUri: string
  secret: string
}

export function setupTwoFactor() {
  return api.post<TwoFactorSetupDto>('/auth/2fa/setup')
}

export function enableTwoFactor(code: string) {
  return api.post<void>('/auth/2fa/enable', { code })
}

export function disableTwoFactor(code: string) {
  return api.post<void>('/auth/2fa/disable', { code })
}

export type UpdateUserPayload = {
  userName: string
  email: string
  role: string
}

export type UpdateProfilePayload = {
  userName: string
  email: string
}

export function getMyProfile() {
  return api.get<UserPublicProfileDto>('/users/me')
}

export function getUserProfile(idUser: string) {
  return api.get<UserPublicProfileDto>(`/users/${idUser}`)
}

export function updateMyProfile(idUser: string, payload: UpdateProfilePayload) {
  return api.put<UserPublicProfileDto>(`/users/${idUser}`, payload)
}

export function uploadMyAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return api.postForm<UserPublicProfileDto>('/users/me/avatar', form)
}

export function getAllUsers() {
  return api.get<UserPublicProfileDto[]>('/users')
}

export function updateUser(idUser: string, payload: UpdateUserPayload) {
  return api.post<UserPublicProfileDto>(`/users/${idUser}`, payload)
}

export function deleteUser(idUser: string) {
  return api.post<void>(`/users/delete/${idUser}`)
}

export type FriendshipStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'BLOCKED'

export type FriendListDto = {
  idUser: string
  userName: string
  createAt: string | null
  status: FriendshipStatus
  senderId: string
  online: boolean
  lastSeen: string | null
  blockerId: string | null
}

export function getFriendList() {
  return api.get<FriendListDto[]>('/friend-list/me/list')
}

export function sendFriendRequest(userName: string) {
  return api.post<void>('/friend-list/send-request', { userName })
}

export function acceptFriendRequest(idUser: string) {
  return api.post<void>(`/friend-list/${idUser}/accept`)
}

export function declineFriendRequest(idUser: string) {
  return api.post<void>(`/friend-list/${idUser}/decline`)
}

export function deleteFriend(idUser: string) {
  return api.post<void>(`/friend-list/${idUser}/delete`)
}

export function blockUser(idUser: string) {
  return api.post<void>(`/friend-list/${idUser}/block`)
}

export function unblockUser(idUser: string) {
  return api.post<void>(`/friend-list/${idUser}/unblock`)
}

export function sendChessInvitation(opponentUsername: string, side: ChessSide | null) {
  return api.post<ChessInvitationDto>('/chess/invitations', { opponentUsername, side })
}

export function getPendingChessInvitations() {
  return api.get<ChessInvitationDto[]>('/chess/invitations/pending')
}

export function acceptChessInvitation(opponentUsername: string) {
  return api.post<ChessMatchDto>('/chess/invitations/accept', { opponentUsername })
}

export function declineChessInvitation(opponentUsername: string) {
  return api.post<void>('/chess/invitations/decline', { opponentUsername })
}

export function createBotMatch(side: ChessSide, botLvl: number) {
  return api.post<ChessMatchDto>('/chess/bot/createBotMatch', { side, botLvl })
}

export function getMyChessMatch() {
  return api.get<ChessMatchDto>('/chess/matches/me')
}

export function playChessMove(matchId: number, move: string) {
  return api.post<ChessMatchDto>(`/chess/matches/${matchId}/moves`, { move })
}

export function resignChessMatch(matchId: number) {
  return api.post<ChessMatchDto>(`/chess/matches/${matchId}/resign`)
}

export function createUndercoverRoom() {
  return api.post<UndercoverRoom>('/undercover/rooms')
}

export function joinUndercoverRoom(code: string) {
  return api.post<UndercoverRoom>('/undercover/rooms/join', { code })
}

export function leaveUndercoverRoom() {
  return api.post<void>('/undercover/rooms/leave')
}

export function configureUndercoverRoom(mode: UndercoverGameMode, rules: UndercoverRules | null) {
  return api.put<UndercoverRoom>('/undercover/rooms/settings', { mode, rules })
}

export function chooseUndercoverWords(categories: string[], wordPairs: UndercoverWordPair[]) {
  return api.put<UndercoverRoom>('/undercover/rooms/words', { categories, wordPairs })
}

export function getUndercoverCategories() {
  return api.get<string[]>('/undercover/rooms/categories')
}

export function startUndercoverMatch() {
  return api.post<UndercoverMatchView>('/undercover/rooms/start')
}

export function getMyUndercoverRoom() {
  return api.get<UndercoverRoom>('/undercover/rooms/me')
}

export function getMyUndercoverMatch() {
  return api.get<UndercoverMatchView>('/undercover/matches/me')
}

export function submitUndercoverClue(matchId: number, word: string) {
  return api.post<UndercoverMatchView>(`/undercover/matches/${matchId}/clues`, { word })
}

export function submitUndercoverVote(matchId: number, targetSeat: number) {
  return api.post<UndercoverMatchView>(`/undercover/matches/${matchId}/votes`, { targetSeat })
}

export function submitUndercoverGuess(matchId: number, guess: string) {
  return api.post<UndercoverMatchView>(`/undercover/matches/${matchId}/guess`, { guess })
}

export function leaveUndercoverMatch(matchId: number) {
  return api.post<void>(`/undercover/matches/${matchId}/leave`)
}

export function sendUndercoverChat(matchId: number, text: string) {
  return api.post<void>(`/undercover/matches/${matchId}/chat`, { text })
}

export function getMyUndercoverStats() {
  return api.get<UndercoverStats>('/undercover/stats/me')
}

export type ChessPlayerStatDto = {
  playerName: string
  winCount: number
  lossCount: number
  drawCount: number
}

export type ChessResult = 'WIN' | 'LOSS' | 'DRAW'

export type ChessHistoryDto = {
  chessMatchId: number
  fen: string
  player1: string
  player2: string
  result: ChessResult
  createdAt: string
  endedAt: string | null
}

export function getMyChessStats() {
  return api.get<ChessPlayerStatDto>('/chess/stats/me')
}

export function getMyChessHistory() {
  return api.get<ChessHistoryDto[]>('/chess/historique/me')
}

export function loginWith42() {
  window.location.href = '/api/auth/42/login'
}

export function link42Account() {
  window.location.href = '/api/auth/42/link'
}

export function unlink42Account() {
  return api.post<void>('/auth/42/unlink')
}

export type MessageResponse = { message: string }

export function forgotPassword(email: string) {
  return api.post<MessageResponse>('/auth/forgot-password', { email })
}

export function resetPassword(token: string, password: string, confirmedPassword: string) {
  return api.post<MessageResponse>('/auth/reset-password', { token, password, confirmedPassword })
}
