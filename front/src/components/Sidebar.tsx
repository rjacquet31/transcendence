import type { LucideIcon } from 'lucide-react'
import {
  ChartBar,
  Crown,
  House,
  MessageCircle,
  Settings,
  Shield,
  User,
  Users,
  VenetianMask,
} from 'lucide-react'
import { Button } from './Button.tsx'
import { UserAvatar } from './UserAvatar.tsx'
import { useAuthModal } from '../auth/authModalContext.ts'
import { useChat } from '../chat/chatContext.ts'
import s from './Sidebar.module.css'

const RECENT_LIMIT = 5

type NavItem = {
  action: string
  label: string
  icon: LucideIcon
}

type NavGroup = {
  title: string
  items: NavItem[]
}

const GROUPS: NavGroup[] = [
  {
    title: 'Navigation',
    items: [{ action: 'nav:home', label: 'Accueil', icon: House }],
  },
  {
    title: 'Jeux',
    items: [
      { action: 'nav:chess', label: 'Échecs', icon: Crown },
      { action: 'nav:undercover', label: 'Undercover', icon: VenetianMask },
    ],
  },
  {
    title: 'Social',
    items: [
      { action: 'nav:friends', label: 'Amis', icon: Users },
      { action: 'nav:chat', label: 'Chat', icon: MessageCircle },
    ],
  },
  {
    title: 'Compte',
    items: [
      { action: 'nav:stats', label: 'Stats & historique', icon: ChartBar },
      { action: 'nav:profile', label: 'Profil', icon: User },
      { action: 'nav:settings', label: 'Paramètres', icon: Settings },
    ],
  },
]

const ADMIN_GROUP: NavGroup = {
  title: 'Administration',
  items: [{ action: 'nav:admin', label: 'Admin', icon: Shield }],
}

const BADGE_LABELS: Record<string, string> = {
  'nav:friends': 'demandes d’ami en attente',
  'nav:chat': 'messages non lus',
}

type SidebarProps = {
  open: boolean
  current: string
  onNavigate: (action: string) => void
}

export function Sidebar({ open, current, onNavigate }: SidebarProps) {
  const { user } = useAuthModal()
  const { rows, recentIds, unread, nameOf, avatarOf, openQuickChat } = useChat()

  const groups = user?.role === 'ADMIN' ? [...GROUPS, ADMIN_GROUP] : GROUPS
  const recent = recentIds.slice(0, RECENT_LIMIT)

  const badges: Record<string, number> = {
    'nav:friends': rows.filter((row) => row.status === 'PENDING' && row.senderId === row.idUser)
      .length,
    'nav:chat': Object.values(unread).reduce((total, count) => total + count, 0),
  }

  return (
    <aside
      id="sidebar"
      className={open ? `${s.sidebar} ${s.open}` : s.sidebar}
      aria-label="Menu principal"
    >
      {groups.map((group) => (
        <nav key={group.title} className={s.group} aria-label={group.title}>
          <p className={s.groupTitle}>{group.title}</p>
          <ul className={s.list}>
            {group.items.map((item) => (
              <li key={item.action}>
                <span className={item.action === current ? `${s.item} ${s.itemActive}` : s.item}>
                  <Button variant="nav" fullWidth onClick={() => onNavigate(item.action)}>
                    <span aria-hidden="true" className={s.icon}>
                      <item.icon size={18} />
                    </span>
                    <span className={s.label}>{item.label}</span>
                    {badges[item.action] ? (
                      <span className={s.badge}>
                        {badges[item.action]}
                        <span className="sr-only"> {BADGE_LABELS[item.action]}</span>
                      </span>
                    ) : null}
                  </Button>
                </span>

                {item.action === 'nav:chat' && recent.length > 0 && (
                  <ul className={s.subList} aria-label="Conversations récentes">
                    {recent.map((idUser) => {
                      const name = nameOf(idUser)
                      const count = unread[idUser] ?? 0

                      return (
                        <li key={idUser}>
                          <button
                            type="button"
                            className={s.subItem}
                            onClick={() => openQuickChat(idUser)}
                            aria-label={`Ouvrir un quick chat avec ${name}`}
                          >
                            <UserAvatar userName={name} avatarUrl={avatarOf(idUser)} size={20} />
                            <span className={s.subLabel}>{name}</span>
                            {count > 0 && <span className={s.subBadge}>{count}</span>}
                          </button>
                        </li>
                      )
                    })}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </nav>
      ))}
    </aside>
  )
}
