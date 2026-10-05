import { useState } from 'react'
import { avatarSrc } from '../lib/avatar.ts'
import s from './UserAvatar.module.css'

function initialsOf(userName: string) {
  const trimmed = userName.trim()
  return trimmed.length > 0 ? trimmed.slice(0, 2).toUpperCase() : '?'
}

type UserAvatarProps = {
  userName: string
  avatarUrl?: string | null
  size?: number
}

export function UserAvatar({ userName, avatarUrl, size = 28 }: UserAvatarProps) {
  const src = avatarSrc(avatarUrl)
  /* On retient l'image qui a échoué : une nouvelle `src` retente d'elle-même. */
  const [failedSrc, setFailedSrc] = useState<string | null>(null)
  const failed = failedSrc === src

  const style = {
    width: `${size}px`,
    height: `${size}px`,
    fontSize: `${Math.max(9, Math.round(size * 0.36))}px`,
  }

  if (!src || failed) {
    return (
      <span className={s.fallback} style={style} aria-hidden="true">
        {initialsOf(userName)}
      </span>
    )
  }

  return (
    <img
      className={s.image}
      style={style}
      src={src}
      alt=""
      width={size}
      height={size}
      onError={() => setFailedSrc(src)}
    />
  )
}