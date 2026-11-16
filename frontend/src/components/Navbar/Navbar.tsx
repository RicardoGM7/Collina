import { User } from 'lucide-react'
import './Navbar.css'

interface NavbarProps {
  userName?: string
  userRole?: string
}

export function Navbar({
  userName = 'Nome do Titular',
  userRole = 'Administrador',
}: NavbarProps) {
  return (
    <header className="navbar">
      <div className="navbar-spacer" />

      <div className="navbar-brand">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" fill="#FFFFFF" />
          <path d="M2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12H2Z" fill="#2BB673" />
          <rect x="2" y="11" width="20" height="2" fill="#0B1724" />
        </svg>
        <div className="navbar-brand-text">
          <span className="navbar-title">Collina</span>
          <span className="navbar-subtitle">Sports Club</span>
        </div>
      </div>

      <div className="navbar-profile">
        <div className="navbar-avatar">
          <User size={18} />
        </div>
        <div className="navbar-user">
          <span className="navbar-user-name">{userName}</span>
          <span className="navbar-user-role">{userRole}</span>
        </div>
      </div>
    </header>
  )
}
