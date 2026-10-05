import type { ReactNode } from 'react'
import s from './Button.module.css'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'nav' | '42'

/* Une classe CSS ne peut pas commencer par un chiffre : on garde `42` côté API. */
const VARIANT_CLASS: Record<ButtonVariant, string> = {
  primary: s.primary,
  secondary: s.secondary,
  ghost: s.ghost,
  nav: s.nav,
  '42': s.ft42,
}

type ButtonProps = {
  children: ReactNode
  onClick?: () => void
  disabled?: boolean
  variant?: ButtonVariant
  fullWidth?: boolean
  ariaLabel?: string
  type?: 'button' | 'submit'
}

export function Button({
  children,
  onClick,
  disabled = false,
  variant = 'secondary',
  fullWidth = false,
  ariaLabel,
  type = 'button',
}: ButtonProps) {
  const classes = [s.btn, VARIANT_CLASS[variant]]
  if (fullWidth) classes.push(s.full)

  return (
    <button
      type={type}
      className={classes.join(' ')}
      onClick={onClick}
      disabled={disabled}
      aria-label={ariaLabel}
    >
      {children}
    </button>
  )
}
