import { api } from './apiClient.ts'

export function deleteMyAccount() {
  return api.post<void>('/users/me/delete')
}