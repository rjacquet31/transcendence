import { useNavigate } from 'react-router-dom'
import { useAuthModal } from '../auth/authModalContext.ts'
import { UserAvatar } from './UserAvatar.tsx'
import s from './AuthStatus.module.css'

export function AuthStatus() {
  const { user } = useAuthModal()
  const navigate = useNavigate()

  if (!user) return null

  return (
    <button
      type="button"
      className={s.status}
      onClick={() => navigate('/profile')}
      aria-label={`Profil de ${user.userName}`}
    >
      <UserAvatar userName={user.userName} avatarUrl={user.avatarUrl} size={22} />
      <span className={s.text}>Connecté</span>
    </button>
  )
}
